package com.tech_mel.tech_mel.application.jobs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.tech_mel.tech_mel.domain.port.input.AuditUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditCleanupScheduler {

    private final AuditUseCase auditUseCase;

    @Value("${app.audit.retention-days:365}")
    private int retentionDays;

    // Executa todos os domingos às 2:00 AM
    @Scheduled(cron = "0 0 2 * * SUN")
    public void cleanupOldAuditRecords() {
        log.info("Iniciando limpeza automática de logs de auditoria. Retenção: {} dias", retentionDays);
        
        try {
            auditUseCase.cleanupOldAuditRecords(retentionDays);
            log.info("Limpeza de logs de auditoria concluída com sucesso");
        } catch (Exception e) {
            log.error("Erro durante a limpeza automática de logs de auditoria: {}", e.getMessage(), e);
        }
    }

    // Executa na inicialização da aplicação (apenas uma vez)
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    public void initialCleanup() {
        log.info("Executando limpeza inicial de logs de auditoria");
        cleanupOldAuditRecords();
    }
}
