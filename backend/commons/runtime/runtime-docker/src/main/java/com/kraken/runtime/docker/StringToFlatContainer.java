package com.kraken.runtime.docker;

import com.kraken.runtime.entity.ContainerStatus;
import com.kraken.runtime.entity.FlatContainer;
import com.kraken.runtime.entity.TaskType;
import com.kraken.tools.environment.KrakenEnvironmentLabels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;

@Component
@Slf4j
final class StringToFlatContainer implements Function<String, FlatContainer> {

  public static String FORMAT = String.format("{{.ID}};{{.Names}};{{.CreatedAt}};{{.Label \"%s\"}};{{.Label \"%s\"}};{{.Label \"%s\"}};{{.Label \"%s\"}};{{.Label \"%s\"}};{{.Label \"%s\"}};{{.Label \"%s\"}}",
      KrakenEnvironmentLabels.COM_KRAKEN_TASK_ID,
      KrakenEnvironmentLabels.COM_KRAKEN_TASK_TYPE,
      KrakenEnvironmentLabels.COM_KRAKEN_CONTAINER_NAME,
      KrakenEnvironmentLabels.COM_KRAKEN_HOST_ID,
      KrakenEnvironmentLabels.COM_KRAKEN_EXPECTED_COUNT,
      KrakenEnvironmentLabels.COM_KRAKEN_LABEL,
      KrakenEnvironmentLabels.COM_KRAKEN_DESCRIPTION);
  private static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z z";

  @Override
  public FlatContainer apply(final String str) {
    final var split = str.split("[;]", 10);
    final var id = split[0];
    final var status = split[1];
    final var dateStr = split[2];
    final var taskId = split[3];
    final var taskType = split[4];
    final var name = split[5];
    final var hostId = split[6];
    final var expectedCount = split[7];
    final var label = split[8];
    final var description = split[9];

    var date = new Date().getTime();
    try {
      date = new SimpleDateFormat(DATE_FORMAT).parse(dateStr).getTime();
    } catch (ParseException e) {
      log.error("Failed to parse container date", e);
    }

    return FlatContainer.builder()
        .id(id)
        .hostId(hostId)
        .taskId(taskId)
        .taskType(TaskType.valueOf(taskType))
        .label(label)
        .name(name)
        .description(description)
        .startDate(date)
        .status(ContainerStatus.parse(status))
        .expectedCount(Integer.valueOf(expectedCount))
        .build();
  }
}
