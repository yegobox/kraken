package com.kraken.gatling.recorder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.NullPointerTester;
import com.kraken.runtime.client.RuntimeClient;
import com.kraken.runtime.command.Command;
import com.kraken.runtime.command.CommandService;
import com.kraken.runtime.command.CommandTest;
import com.kraken.runtime.container.properties.RuntimeContainerProperties;
import com.kraken.runtime.container.properties.RuntimeContainerPropertiesTest;
import com.kraken.runtime.entity.*;
import com.kraken.runtime.gatling.GatlingExecutionProperties;
import com.kraken.runtime.gatling.GatlingExecutionPropertiesTest;
import com.kraken.storage.client.StorageClient;
import com.kraken.storage.entity.StorageNodeTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.function.Supplier;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GatlingRecorderTest {

  @Mock
  StorageClient storageClient;
  @Mock
  RuntimeClient runtimeClient;
  @Mock
  CommandService commandService;
  @Mock
  Supplier<Command> commandSupplier;
  RuntimeContainerProperties containerProperties;
  GatlingExecutionProperties gatlingExecutionProperties;

  GatlingRecorder recorder;

  @Before
  public void before() {
    given(commandSupplier.get()).willReturn(CommandTest.SHELL_COMMAND);
    containerProperties = RuntimeContainerPropertiesTest.RUNTIME_PROPERTIES;
    gatlingExecutionProperties = GatlingExecutionPropertiesTest.GATLING_PROPERTIES;
    recorder = new GatlingRecorder(storageClient,
        runtimeClient,
        commandService,
        containerProperties,
        gatlingExecutionProperties,
        commandSupplier);
  }

  @Test
  public void shouldInit() {
    given(runtimeClient.find(containerProperties.getTaskId(), containerProperties.getContainerName())).willReturn(Mono.just(FlatContainerTest.CONTAINER));
    given(runtimeClient.setStatus(any(FlatContainer.class), any(ContainerStatus.class))).willReturn(Mono.fromCallable(() -> null));
    given(runtimeClient.waitForStatus(anyString(), any(ContainerStatus.class))).willReturn(Mono.just(TaskTest.TASK));
    given(storageClient.downloadFolder(any(Path.class), any())).willReturn(Mono.fromCallable(() -> null));
    given(storageClient.downloadFile(any(Path.class), any())).willReturn(Mono.fromCallable(() -> null));
    given(storageClient.uploadFile(any(Path.class), any())).willReturn(Mono.just(StorageNodeTest.STORAGE_NODE));
    given(commandService.execute(any(Command.class))).willReturn(Flux.just("cmd", "exec", "logs"));
    recorder.init();
    verify(runtimeClient).setStatus(FlatContainerTest.CONTAINER, ContainerStatus.PREPARING);
    verify(runtimeClient).setStatus(FlatContainerTest.CONTAINER, ContainerStatus.READY);
    verify(runtimeClient).setStatus(FlatContainerTest.CONTAINER, ContainerStatus.RUNNING);
    verify(runtimeClient).setStatus(FlatContainerTest.CONTAINER, ContainerStatus.STOPPING);
    verify(runtimeClient).setStatus(FlatContainerTest.CONTAINER, ContainerStatus.DONE);
    verify(runtimeClient).waitForStatus(containerProperties.getTaskId(), ContainerStatus.READY);
    verify(runtimeClient).waitForStatus(containerProperties.getTaskId(), ContainerStatus.STOPPING);
    verify(storageClient).downloadFolder(any(Path.class), any());
    verify(storageClient).downloadFile(any(Path.class), any());
    verify(storageClient).uploadFile(any(Path.class), any());
    verify(commandService).execute(Command.builder()
        .path(gatlingExecutionProperties.getGatlingHome().toString())
        .command(ImmutableList.of("ls", "-lR"))
        .environment(ImmutableMap.of())
        .build());
    verify(commandService).execute(CommandTest.SHELL_COMMAND);

  }

  @Test
  public void shouldPassTestUtils() {
    new NullPointerTester()
        .setDefault(RuntimeContainerProperties.class, containerProperties)
        .setDefault(GatlingExecutionProperties.class, gatlingExecutionProperties)
        .testConstructors(GatlingRecorder.class, PACKAGE);
  }
}
