package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.port.input.HiveUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.CreateHiveRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.hive.HiveResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hives")
@RequiredArgsConstructor
public class HiveController {
    private final HiveUseCase hiveUseCase;

    @PostMapping()
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
}
