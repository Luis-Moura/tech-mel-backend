package com.tech_mel.tech_mel.application.jobs;

import com.tech_mel.tech_mel.domain.model.DailyMeasurementAverage;
import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.model.Measurement;
import com.tech_mel.tech_mel.domain.port.output.DailyMeasurementAverageRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.HiveRepositoryPort;
import com.tech_mel.tech_mel.domain.port.output.RedisIotPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DailyAverageScheduler {
    private final HiveRepositoryPort hiveRepositoryPort;
    private final RedisIotPort redisIotPort;
    private final DailyMeasurementAverageRepositoryPort dailyMeasurementAverageRepositoryPort;

    @Scheduled(cron = "0 1 0 * * *") //todos os dias à meia noite
    public void processDailyAverages() {
        log.info("Starting daily average processing...");

        List<Hive> allHives = hiveRepositoryPort.findAllHives(Pageable.unpaged()).getContent();

        for (Hive hive : allHives) {
            String apiKey = hive.getApiKey();

            List<Measurement> measurements = redisIotPort.getMeasurements(apiKey, 1000);

            List<Measurement> last24HoursMeasurements = measurements.stream()
                    .filter(measurement -> measurement.getMeasuredAt().isAfter(LocalDateTime.now().minusHours(24)))
                    .toList();

            if (last24HoursMeasurements.isEmpty()) {
                continue;
            }

            double averageTemperature = last24HoursMeasurements.stream()
                    .mapToDouble(Measurement::getTemperature)
                    .average()
                    .orElse(0.0);

            double averageHumidity = last24HoursMeasurements.stream()
                    .mapToDouble(Measurement::getHumidity)
                    .average()
                    .orElse(0.0);

            double averageCo2 = last24HoursMeasurements.stream()
                    .mapToDouble(Measurement::getCo2)
                    .average()
                    .orElse(0.0);

            DailyMeasurementAverage dailyMeasurementAverage = DailyMeasurementAverage.builder()
                    .hive(hive)
                    .avgTemperature(averageTemperature)
                    .avgHumidity(averageHumidity)
                    .avgCo2(averageCo2)
                    .date(LocalDate.now().minusDays(1))
                    .build();

            dailyMeasurementAverageRepositoryPort.save(dailyMeasurementAverage);
            redisIotPort.clearMeasurements(apiKey);
        }
    }
}
