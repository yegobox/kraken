package com.kraken.runtime.server.service;

import com.kraken.runtime.api.TaskService;
import com.kraken.runtime.entity.*;
import lombok.NonNull;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class SpringTaskListServiceTest {

  @Mock
  FlatContainersToTask toTask;
  @Mock
  TaskService taskService;

  TaskListService taskListService;

  @Before
  public void before() {
    this.taskListService = new SpringTaskListService(toTask, taskService);
  }

  @Test
  public void shouldList() {
    given(taskService.list()).willReturn(Flux.just(FlatContainerTest.CONTAINER));
    given(toTask.apply(any())).willReturn(Mono.just(TaskTest.TASK));

    assertThat(taskListService.list().blockFirst()).isEqualTo(TaskTest.TASK);
  }

  @Test
  public void shouldWatch() {
    given(taskService.list()).willReturn(Flux.just(FlatContainerTest.CONTAINER));
    given(toTask.apply(any())).willReturn(Mono.just(TaskTest.TASK));

    final var tasks = taskListService.watch().take(SpringTaskListService.WATCH_TASKS_DELAY.multipliedBy(3)).collectList().block();
    assertThat(tasks).isNotNull();
    assertThat(tasks.size()).isEqualTo(1);
  }

}

