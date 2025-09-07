package com.tech_mel.tech_mel.infrastructure.api.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tech_mel.tech_mel.domain.model.AuditLog;
import com.tech_mel.tech_mel.domain.port.input.AuditUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.admin.AuditLogFilterRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.admin.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit", description = "Operações de auditoria do sistema")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditUseCase auditUseCase;

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar logs de auditoria", description = "Lista logs de auditoria com paginação e filtros")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @Parameter(description = "Página (inicia em 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo para ordenação") @RequestParam(defaultValue = "timestamp") String sortBy,
            @Parameter(description = "Direção da ordenação") @RequestParam(defaultValue = "desc") String sortDirection,
            AuditLogFilterRequest filters) {
        
        log.info("Buscando logs de auditoria - página: {}, tamanho: {}", page, size);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<AuditLog> auditLogs = auditUseCase.getAuditLogsWithFilters(
            filters.getUserId(),
            filters.getAction(),
            filters.getEntityType(),
            filters.getStartDate(),
            filters.getEndDate(),
            filters.getSuccess(),
            pageable
        );
        
        Page<AuditLogResponse> response = auditLogs.map(this::mapToAuditLogResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Logs por usuário", description = "Retorna logs de auditoria de um usuário específico")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByUser(
            @PathVariable UUID userId,
            @Parameter(description = "Página (inicia em 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "20") int size) {
        
        log.info("Buscando logs de auditoria para usuário: {}", userId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogs = auditUseCase.getAuditLogsByUser(userId, pageable);
        
        Page<AuditLogResponse> response = auditLogs.map(this::mapToAuditLogResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/recent/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atividade recente do usuário", description = "Retorna atividade recente de um usuário")
    public ResponseEntity<List<AuditLogResponse>> getRecentUserActivity(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Buscando atividade recente do usuário: {}", userId);
        
        List<AuditLog> recentActivity = auditUseCase.getRecentUserActivity(userId, limit);
        List<AuditLogResponse> response = recentActivity.stream()
                .map(this::mapToAuditLogResponse)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Logs por ação", description = "Retorna logs de auditoria por tipo de ação")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByAction(
            @PathVariable String action,
            @Parameter(description = "Página (inicia em 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "20") int size) {
        
        log.info("Buscando logs de auditoria para ação: {}", action);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<AuditLog> auditLogs = auditUseCase.getAuditLogsByAction(
                com.tech_mel.tech_mel.domain.model.AuditAction.valueOf(action.toUpperCase()), 
                pageable
            );
            
            Page<AuditLogResponse> response = auditLogs.map(this::mapToAuditLogResponse);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Ação de auditoria inválida: {}", action);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/logs/entity/{entityType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Logs por tipo de entidade", description = "Retorna logs de auditoria por tipo de entidade")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByEntityType(
            @PathVariable String entityType,
            @Parameter(description = "Página (inicia em 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "20") int size) {
        
        log.info("Buscando logs de auditoria para tipo de entidade: {}", entityType);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
            Page<AuditLog> auditLogs = auditUseCase.getAuditLogsByEntityType(
                com.tech_mel.tech_mel.domain.model.EntityType.valueOf(entityType.toUpperCase()), 
                pageable
            );
            
            Page<AuditLogResponse> response = auditLogs.map(this::mapToAuditLogResponse);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Tipo de entidade inválido: {}", entityType);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estatísticas de auditoria", description = "Retorna estatísticas dos logs de auditoria")
    public ResponseEntity<Map<String, Object>> getAuditStatistics() {
        log.info("Buscando estatísticas de auditoria");
        
        Map<String, Long> actionStats = auditUseCase.getAuditStatisticsByAction();
        Map<String, Long> entityStats = auditUseCase.getAuditStatisticsByEntityType();
        long totalRecords = auditUseCase.getTotalAuditRecords();
        long failedActions = auditUseCase.getFailedActionsCount();
        
        Map<String, Object> statistics = Map.of(
            "totalRecords", totalRecords,
            "failedActions", failedActions,
            "successRate", totalRecords > 0 ? ((double)(totalRecords - failedActions) / totalRecords) * 100 : 0,
            "actionStatistics", actionStats,
            "entityStatistics", entityStats,
            "generatedAt", LocalDateTime.now()
        );
        
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/logs/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exportar logs", description = "Exporta logs de auditoria em um período")
    public ResponseEntity<List<AuditLogResponse>> exportAuditLogs(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        
        log.info("Exportando logs de auditoria de {} até {}", startDate, endDate);
        
        List<AuditLog> auditLogs = auditUseCase.exportAuditLogs(startDate, endDate);
        List<AuditLogResponse> response = auditLogs.stream()
                .map(this::mapToAuditLogResponse)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/maintenance/cleanup")
    @PreAuthorize("hasRole('ADMIN') and @adminService.isPrimaryAdmin(authentication.principal.id)")
    @Operation(summary = "Limpeza de logs antigos", description = "Remove logs de auditoria antigos (apenas admin primário)")
    public ResponseEntity<Void> cleanupOldAuditRecords(
            @RequestParam(defaultValue = "365") int retentionDays) {
        
        log.info("Iniciando limpeza de logs de auditoria - retenção: {} dias", retentionDays);
        auditUseCase.cleanupOldAuditRecords(retentionDays);
        return ResponseEntity.ok().build();
    }

    // ========== MÉTODOS AUXILIARES ==========

    private AuditLogResponse mapToAuditLogResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .userName(auditLog.getUserName())
                .userEmail(auditLog.getUserEmail())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .details(auditLog.getDetails())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .timestamp(auditLog.getTimestamp())
                .oldValues(auditLog.getOldValues())
                .newValues(auditLog.getNewValues())
                .success(auditLog.isSuccess())
                .errorMessage(auditLog.getErrorMessage())
                .build();
    }
}
