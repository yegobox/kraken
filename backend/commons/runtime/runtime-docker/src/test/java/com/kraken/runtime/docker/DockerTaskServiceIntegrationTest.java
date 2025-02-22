package com.kraken.runtime.docker;

import com.google.common.collect.ImmutableMap;
import com.kraken.runtime.command.Command;
import com.kraken.runtime.command.CommandService;
import com.kraken.runtime.entity.FlatContainerTest;
import com.kraken.runtime.entity.TaskType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class}, initializers = {ConfigFileApplicationContextInitializer.class})
@EnableAutoConfiguration
public class DockerTaskServiceIntegrationTest {

  @Autowired
  DockerTaskService taskService;

  @Autowired
  CommandService commandService;

  @Before
  public void before() {
    final var up = Command.builder()
        .path(Paths.get("testDir").toAbsolutePath().toString())
        .command(Arrays.asList("docker-compose", "up", "-d"))
        .environment(ImmutableMap.of())
        .build();
    commandService.execute(up).blockLast();

    final var clean = Command.builder()
        .path(Paths.get("testDir").toAbsolutePath().toString())
        .command(Arrays.asList("/bin/sh", "-c", "docker rm -v $(docker ps -a -q -f status=exited)"))
        .environment(ImmutableMap.of())
        .build();
    commandService.execute(clean).blockLast();
  }

  @After
  public void after() {
    final var down = Command.builder()
        .path(Paths.get("testDir").toAbsolutePath().toString())
        .command(Arrays.asList("docker-compose", "down"))
        .environment(ImmutableMap.of())
        .build();
    commandService.execute(down).blockLast();
  }

  @Test
  public void shouldListFlatContainers() {
    final var flatContainers = taskService.list().collectList().block();
    assertThat(flatContainers).isNotNull();
    System.out.println(flatContainers);
    assertThat(flatContainers.size()).isEqualTo(3);
    final var debugContainers = flatContainers.stream().filter(flatContainer -> flatContainer.getTaskType() == TaskType.DEBUG).collect(Collectors.toList());
    final var debugContainer = debugContainers.stream().findFirst().orElse(FlatContainerTest.CONTAINER);
    final var runContainers = flatContainers.stream().filter(flatContainer -> flatContainer.getTaskType() == TaskType.RUN).collect(Collectors.toList());
    final var runContainer = runContainers.stream().findFirst().orElse(FlatContainerTest.CONTAINER);
    assertThat(debugContainer.getTaskId()).isEqualTo("taskId");
    assertThat(debugContainer.getDescription()).isEqualTo("Debug Task");
    assertThat(debugContainers.size()).isEqualTo(2);
    assertThat(runContainer.getTaskId()).isEqualTo("taskIdBis");
    assertThat(runContainers.size()).isEqualTo(1);
  }

}
