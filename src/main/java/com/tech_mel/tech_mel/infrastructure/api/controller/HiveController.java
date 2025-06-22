package com.tech_mel.tech_mel.infrastructure.api.controller;

import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.port.input.HiveUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.CreateHiveRequest;
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
@RequestMapping("/api/hives")
@RequiredArgsConstructor
public class HiveController {
    private final HiveUseCase hiveUseCase;
    private final AuthenticationUtil authenticationUtil;

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

    @GetMapping("/my-hives")
    public ResponseEntity<Page<GetMyHivesResponse>> getMyhives(Pageable pageable) {
        UUID userId = authenticationUtil.getCurrentUserId();

        Page<Hive> page = hiveUseCase.listMyHives(userId, pageable);

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
}
