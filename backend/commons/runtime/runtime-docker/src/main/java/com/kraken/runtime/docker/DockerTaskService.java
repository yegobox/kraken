package com.kraken.runtime.docker;

import com.google.common.collect.ImmutableMap;
import com.kraken.runtime.api.TaskService;
import com.kraken.runtime.command.Command;
import com.kraken.runtime.command.CommandService;
import com.kraken.runtime.docker.env.EnvironmentChecker;
import com.kraken.runtime.docker.env.EnvironmentPublisher;
import com.kraken.runtime.entity.*;
import com.kraken.runtime.logs.LogsService;
import com.kraken.runtime.server.properties.RuntimeServerProperties;
import com.kraken.tools.properties.ApplicationProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.kraken.tools.environment.KrakenEnvironmentLabels.COM_KRAKEN_TASK_ID;

@Slf4j
@Component
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
final class DockerTaskService implements TaskService {

  public static final String DOCKER_COMPOSE_YML = "docker-compose.yml";
  @NonNull CommandService commandService;
  @NonNull LogsService logsService;
  @NonNull Function<String, FlatContainer> stringToFlatContainer;
  @NonNull ApplicationProperties applicationProperties;
  @NonNull List<EnvironmentChecker> envCheckers;
  @NonNull List<EnvironmentPublisher> envPublishers;

  @Override
  public Mono<ExecutionContext> execute(final ExecutionContext context) {
    checkArgument(context.getHosts().size() <= 1, "The Docker runtime server can only run tasks on one host!");

    final var env = this.updateEnvironment(context);
    envCheckers.stream()
        .filter(environmentChecker -> environmentChecker.test(context.getTaskType()))
        .forEach(environmentChecker -> environmentChecker.accept(env));
    return Mono.fromCallable(() -> {
      final var path = this.createDockerComposeFolder(context);
      final var command = Command.builder()
          .path(path)
          .command(Arrays.asList("docker-compose",
              "--no-ansi",
              "up",
              "-d",
              "--no-color"))
          .environment(env)
          .build();

      // Automatically display logs stream
      final var logs = commandService.execute(command).doOnTerminate(() -> this.removeDockerComposeFolder(context));
      logsService.push(context.getApplicationId(), context.getTaskId(), LogType.TASK, logs);
      return context;
    });
  }

  @Override
  public Mono<String> cancel(final String applicationId, final String taskId, final TaskType taskType) {
    final var context = ExecutionContext.builder()
        .hosts(ImmutableMap.of())
        .taskId(taskId)
        .taskType(taskType)
        .applicationId(applicationId)
        .environment(ImmutableMap.of())
        .description("")
        .build();

    return Mono.fromCallable(() -> {
      final var path = this.createDockerComposeFolder(context);
      final var command = Command.builder()
          .path(path)
          .command(Arrays.asList("docker-compose",
              "--no-ansi",
              "down"))
          .environment(this.updateEnvironment(context))
          .build();
      final var logs = commandService.execute(command).doOnTerminate(() -> this.removeDockerComposeFolder(context));
      logsService.push(applicationId, taskId, LogType.TASK, logs);
      return taskId;
    });
  }

  @Override
  public Flux<FlatContainer> list() {
    final var command = Command.builder()
        .path(".")
        .command(Arrays.asList("docker",
            "ps",
            "-a",
            "--filter", String.format("label=%s", COM_KRAKEN_TASK_ID),
            "--format", StringToFlatContainer.FORMAT))
        .environment(ImmutableMap.of())
        .build();

    return commandService.execute(command)
        .map(stringToFlatContainer);
  }

  private Map<String, String> updateEnvironment(final ExecutionContext context) {
    // Update the env variable with all useful key/values
    final var envBuilder = ImmutableMap.<String, String>builder();
    envBuilder.putAll(context.getEnvironment());
    envPublishers.stream()
        .filter(environmentPublisher -> environmentPublisher.test(context.getTaskType()))
        .forEach(environmentPublisher -> envBuilder.putAll(environmentPublisher.apply(context)));
    return envBuilder.build();
  }


  private String createDockerComposeFolder(final ExecutionContext context) throws IOException {
    final var taskPath = Files.createDirectory(this.applicationProperties.getData().resolve(context.getTaskId()));
    final var composePath = this.applicationProperties.getData().resolve(context.getTaskType().toString()).resolve(DOCKER_COMPOSE_YML);
    Files.copy(composePath, taskPath.resolve(DOCKER_COMPOSE_YML));
    return taskPath.toString();
  }

  private void removeDockerComposeFolder(final ExecutionContext context) {
    final var taskPath = this.applicationProperties.getData().resolve(context.getTaskId());
    try {
      FileSystemUtils.deleteRecursively(taskPath);
    } catch (IOException e) {
      log.debug("Failed to remove task folder " + taskPath, e);
    }
  }

}
