package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.port.input.HiveUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.CreateHiveRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.UpdateApiKeyRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.UpdateHiveStatusRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.hive.GetMyHivesResponse;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.hive.HiveResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HiveController {
    private final HiveUseCase hiveUseCase;
    private final AuthenticationUtil authenticationUtil;

    @PostMapping("/technician/hives")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
    public ResponseEntity<HiveResponse> createHive(@Valid @RequestBody CreateHiveRequest request) {
        Hive createdHive = hiveUseCase.createHive(request);

        HiveResponse hiveResponse = HiveResponse.builder()
                .id(createdHive.getId())
                .name(createdHive.getName())
                .location(createdHive.getLocation())
                .apiKey(createdHive.getApiKey())
                .hiveStatus(createdHive.getHiveStatus())
                .ownerId(createdHive.getOwner().getId())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(hiveResponse);
    }

    @GetMapping("/my/hives")
    public ResponseEntity<Page<GetMyHivesResponse>> getMyhives(Pageable pageable) {
        UUID userId = authenticationUtil.getCurrentUserId();

        Page<Hive> page = hiveUseCase.listHivesByOwner(userId, pageable);

        Page<GetMyHivesResponse> response = page.map(hive -> GetMyHivesResponse.builder()
                .id(hive.getId())
                .name(hive.getName())
                .location(hive.getLocation())
                .hiveStatus(hive.getHiveStatus())
                .ownerId(hive.getOwner().getId())
                .build()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/technician/users/{ownerId}/hives")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
    public ResponseEntity<Page<HiveResponse>> getHivesByOwner(@PathVariable UUID ownerId, Pageable pageable) {
        Page<Hive> page = hiveUseCase.listHivesByOwner(ownerId, pageable);

        Page<HiveResponse> response = page.map(hive -> HiveResponse.builder()
                .id(hive.getId())
                .name(hive.getName())
                .location(hive.getLocation())
                .apiKey(hive.getApiKey())
                .hiveStatus(hive.getHiveStatus())
                .ownerId(hive.getOwner().getId())
                .build()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my/hives/{hiveId}")
    public ResponseEntity<GetMyHivesResponse> getMyHiveById(@PathVariable UUID hiveId) {
        UUID ownerId = authenticationUtil.getCurrentUserId();

        Hive hive = hiveUseCase.getHiveById(hiveId, ownerId);

        GetMyHivesResponse response = GetMyHivesResponse.builder()
                .id(hive.getId())
                .name(hive.getName())
                .location(hive.getLocation())
                .hiveStatus(hive.getHiveStatus())
                .ownerId(hive.getOwner().getId())
                .build();

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/technician/api-key/{hiveId}")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
    public ResponseEntity<Void> updateApiKey(
            @PathVariable UUID hiveId,
            @Valid @RequestBody UpdateApiKeyRequest request
    ) {
        hiveUseCase.updateApiKey(hiveId, request.apiKey().toString());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/technician/hive-status/{hiveId}")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
    public ResponseEntity<Void> updateHiveStatus(
            @PathVariable UUID hiveId,
            @Valid @RequestBody UpdateHiveStatusRequest request
    ) {
        hiveUseCase.updateHiveStatus(hiveId, request.hiveStatus());

        return ResponseEntity.noContent().build();
    }
}
