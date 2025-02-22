package com.kraken.runtime.server.service;

import com.kraken.runtime.entity.ContainerStatus;
import com.kraken.runtime.entity.FlatContainer;
import com.kraken.runtime.entity.TaskType;
import org.junit.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringFlatContainersToTaskTest {

  private final FlatContainersToTask dockerContainersToTask = new SpringFlatContainersToTask(new SpringFlatContainerToContainer());

  @Test
  public void shouldSetCreatingStatus() {
    final var container = FlatContainer.builder()
        .id("id")
        .name("containerId")
        .hostId("hostId")
        .taskId("taskId")
        .taskType(TaskType.RUN)
        .label("name")
        .description("description")
        .startDate(42L)
        .status(ContainerStatus.READY)
        .expectedCount(2)
        .build();
    final var flux = Flux.just(container).groupBy(FlatContainer::getTaskId).next().block();
    assertThat(flux).isNotNull();
    final var task = dockerContainersToTask.apply(flux).block();
    assertThat(task).isNotNull();
    assertThat(task.getStatus()).isEqualTo(ContainerStatus.CREATING);
    assertThat(task.getDescription()).isEqualTo("description");
    assertThat(task.getContainers().size()).isEqualTo(1);
    assertThat(task.getExpectedCount()).isEqualTo(2);
  }

  @Test
  public void shouldKeepRunningStatus() {
    final var container = FlatContainer.builder()
        .id("id")
        .name("containerId")
        .hostId("hostId")
        .taskId("taskId")
        .taskType(TaskType.RUN)
        .label("name")
        .description("description")
        .startDate(42L)
        .status(ContainerStatus.RUNNING)
        .expectedCount(2)
        .build();
    final var flux = Flux.just(container).groupBy(FlatContainer::getTaskId).next().block();
    assertThat(flux).isNotNull();
    final var task = dockerContainersToTask.apply(flux).block();
    assertThat(task).isNotNull();
    assertThat(task.getStatus()).isEqualTo(ContainerStatus.RUNNING);
    assertThat(task.getDescription()).isEqualTo("description");
    assertThat(task.getContainers().size()).isEqualTo(1);
    assertThat(task.getExpectedCount()).isEqualTo(2);
  }

  @Test
  public void shouldUseMinContainerStatus() {
    final var container1 = FlatContainer.builder()
        .id("id1")
        .name("containerId1")
        .hostId("hostId")
        .taskId("taskId")
        .taskType(TaskType.RUN)
        .label("name")
        .description("description")
        .startDate(42L)
        .status(ContainerStatus.READY)
        .expectedCount(2)
        .build();
    final var container2 = FlatContainer.builder()
        .id("id1")
        .name("containerId2")
        .hostId("hostId")
        .taskId("taskId")
        .taskType(TaskType.RUN)
        .label("name")
        .description("description")
        .startDate(42L)
        .status(ContainerStatus.RUNNING)
        .expectedCount(2)
        .build();
    final var flux = Flux.just(container1, container2).groupBy(FlatContainer::getTaskId).next().block();
    assertThat(flux).isNotNull();
    final var task = dockerContainersToTask.apply(flux).block();
    assertThat(task).isNotNull();
    assertThat(task.getStatus()).isEqualTo(ContainerStatus.READY);
    assertThat(task.getDescription()).isEqualTo("description");
    assertThat(task.getContainers().size()).isEqualTo(2);
    assertThat(task.getExpectedCount()).isEqualTo(2);
  }

}

