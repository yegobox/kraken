package com.kraken.runtime.docker.env;

import com.google.common.collect.ImmutableMap;
import com.kraken.runtime.entity.TaskType;
import com.kraken.test.utils.TestUtils;
import com.kraken.tools.environment.KrakenEnvironmentKeys;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {RecordChecker.class})
public class RecordCheckerTest {

  @Autowired
  RecordChecker checker;

  @Test
  public void shouldTest() {
    assertThat(checker.test(TaskType.RUN)).isFalse();
    assertThat(checker.test(TaskType.DEBUG)).isFalse();
    assertThat(checker.test(TaskType.RECORD)).isTrue();
  }


  @Test(expected = NullPointerException.class)
  public void shouldFailCheck() {
    checker.accept(ImmutableMap.of());
  }

  @Test
  public void shouldSucceed() {
    final var env = ImmutableMap.<String, String>builder()
        .put(KrakenEnvironmentKeys.KRAKEN_VERSION, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_GATLING_SIMULATION_CLASS, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_GATLING_SIMULATION_PACKAGE, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_GATLING_HAR_PATH_LOCAL, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_GATLING_HAR_PATH_REMOTE, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_DESCRIPTION, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_TASK_ID, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_EXPECTED_COUNT, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_ANALYSIS_URL, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_RUNTIME_URL, "value")
        .put(KrakenEnvironmentKeys.KRAKEN_STORAGE_URL, "value")
        .build();
    checker.accept(env);
  }

  @Test
  public void shouldTestUtils() {
    TestUtils.shouldPassNPE(RecordChecker.class);
  }
}
