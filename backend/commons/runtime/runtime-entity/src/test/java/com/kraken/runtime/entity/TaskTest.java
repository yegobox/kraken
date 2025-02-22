package com.kraken.runtime.entity;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static com.kraken.test.utils.TestUtils.shouldPassAll;

public class TaskTest {

  public static final Task TASK = Task.builder()
      .id("id")
      .startDate(42L)
      .status(ContainerStatus.STARTING)
      .type(TaskType.RUN)
      .containers(ImmutableList.of())
      .expectedCount(2)
      .description("description")
      .build();


  @Test
  public void shouldPassTestUtils() {
    shouldPassAll(TASK);
  }

}
