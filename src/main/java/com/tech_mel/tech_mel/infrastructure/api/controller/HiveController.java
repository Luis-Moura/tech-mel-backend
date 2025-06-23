package com.tech_mel.tech_mel.infrastructure.api.controller;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tech_mel.tech_mel.domain.model.Hive;
import com.tech_mel.tech_mel.domain.model.User;
import com.tech_mel.tech_mel.domain.port.input.HiveUseCase;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.CreateHiveRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.UpdateApiKeyRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.request.hive.UpdateHiveStatusRequest;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.hive.GetMyHivesResponse;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.hive.HiveResponse;
import com.tech_mel.tech_mel.infrastructure.api.dto.response.hive.UsersWithAvailableHivesResponse;
import com.tech_mel.tech_mel.infrastructure.security.util.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Hives", description = "Endpoints para gestão de colmeias")
@SecurityRequirement(name = "bearerAuth")
public class HiveController {
    private final HiveUseCase hiveUseCase;
    private final AuthenticationUtil authenticationUtil;

    @PostMapping("/technician/hives")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
    @Operation(
            summary = "Criar nova colmeia",
            description = "Permite que técnicos criem novas colmeias para usuários. Requer papel de TECHNICIAN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Colmeia criada com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HiveResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                                                "name": "Colmeia Principal",
                                                "location": "Apiário Norte - Setor A1",
                                                "apiKey": "hive_key_abc123def456",
                                                "hiveStatus": "INACTIVE",
                                                "ownerId": "f47ac10b-58cc-4372-a567-0e02b2c3d478"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou usuário sem colmeias disponíveis",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Colmeias indisponíveis",
                                            value = """
                                                    {
                                                        "timestamp": "2024-01-01T10:00:00",
                                                        "status": 400,
                                                        "error": "Bad Request",
                                                        "message": "Usuário não possui colmeias disponíveis."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Dados inválidos",
                                            value = """
                                                    {
                                                        "timestamp": "2024-01-01T10:00:00",
                                                        "status": 400,
                                                        "error": "Validation Error",
                                                        "message": {
                                                            "name": "O nome da colmeia é obrigatório.",
                                                            "location": "A localização da colmeia é obrigatória."
                                                        }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não tem permissão de técnico",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "Acesso negado."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário dono da colmeia não encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "Usuário dono da colmeia não encontrado"
                                            }
                                            """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Listar minhas colmeias",
            description = "Retorna uma lista paginada das colmeias do usuário autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de colmeias retornada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<Page<GetMyHivesResponse>> getMyhives(
            @Parameter(description = "Parâmetros de paginação", hidden = true)
            Pageable pageable
    ) {
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

    @GetMapping("/technician/hives")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
    @Operation(
            summary = "Listar todas as colmeias",
            description = "Permite que técnicos visualizem todas as colmeias. Requer papel de TECHNICIAN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de colmeias do usuário retornada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não tem permissão de técnico",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "Acesso negado."
                                            }
                                            """
                            )
                    )
            ),
    })
    public ResponseEntity<Page<HiveResponse>> getAllHives(
            @Parameter(description = "Parâmetros de paginação", hidden = true)
            Pageable pageable
    ) {
        Page<Hive> page = hiveUseCase.listAllHives(pageable);

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

    @GetMapping("/technician/available-users")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
    @Operation(
        summary = "Listar usuários com colmeias disponíveis",
        description = "Permite que técnicos visualizem todos os usuários que possuem colmeias disponíveis para criação. Requer papel de TECHNICIAN."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de usuários com colmeias disponíveis retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UsersWithAvailableHivesResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "content": [
                            {
                                "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                                "name": "João Silva",
                                "email": "joao@exemplo.com",
                                "availableHives": 5
                            },
                            {
                                "id": "a47bc20c-68dd-5473-b678-1f13c3d4e590",
                                "name": "Maria Santos",
                                "email": "maria@exemplo.com",
                                "availableHives": 3
                            }
                        ],
                        "pageable": {
                            "sort": {
                                "empty": true,
                                "sorted": false,
                                "unsorted": true
                            },
                            "offset": 0,
                            "pageSize": 20,
                            "pageNumber": 0,
                            "paged": true,
                            "unpaged": false
                        },
                        "last": true,
                        "totalElements": 2,
                        "totalPages": 1,
                        "size": 20,
                        "number": 0,
                        "sort": {
                            "empty": true,
                            "sorted": false,
                            "unsorted": true
                        },
                        "first": true,
                        "numberOfElements": 2,
                        "empty": false
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de acesso inválido ou expirado",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token inválido ou expirado"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Usuário não tem permissão de técnico",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 403,
                        "error": "Forbidden",
                        "message": "Acesso negado."
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Nenhum usuário encontrado com colmeias disponíveis",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "timestamp": "2024-01-01T10:00:00",
                        "status": 404,
                        "error": "Not Found",
                        "message": "Nenhum usuário encontrado"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<Page<UsersWithAvailableHivesResponse>> getUsersWithAvailableHives(
            @Parameter(description = "Parâmetros de paginação", hidden = true)
            Pageable pageable
    ) {
        Page<User> page = hiveUseCase.listAllUsersWithAvailableHives(pageable);

        Page<UsersWithAvailableHivesResponse> response = page
                .map(user -> UsersWithAvailableHivesResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .availableHives(user.getAvailableHives())
                        .build()
                );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/my/hives/{hiveId}")
    @Operation(
            summary = "Obter detalhes de uma colmeia específica",
            description = "Retorna os detalhes de uma colmeia específica do usuário autenticado"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalhes da colmeia retornados com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetMyHivesResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                                                "name": "Colmeia Principal",
                                                "location": "Apiário Norte - Setor A1",
                                                "hiveStatus": "ACTIVE",
                                                "ownerId": "f47ac10b-58cc-4372-a567-0e02b2c3d478"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não tem permissão para acessar esta colmeia",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "Você não tem permissão para acessar esta colmeia."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Colmeia não encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "Hive não encontrada"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<GetMyHivesResponse> getMyHiveById(
            @Parameter(description = "ID da colmeia", required = true)
            @PathVariable UUID hiveId
    ) {
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
    @Operation(
            summary = "Atualizar chave de API da colmeia",
            description = "Permite que técnicos atualizem a chave de API de uma colmeia. Requer papel de TECHNICIAN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Chave de API atualizada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 400,
                                                "error": "Validation Error",
                                                "message": {
                                                    "apiKey": "A chave de API é obrigatória"
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não tem permissão de técnico",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "Acesso negado."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Colmeia não encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "Hive não encontrada"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<Void> updateApiKey(
            @Parameter(description = "ID da colmeia", required = true)
            @PathVariable UUID hiveId,
            @Valid @RequestBody UpdateApiKeyRequest request
    ) {
        hiveUseCase.updateApiKey(hiveId, request.apiKey().toString());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/technician/hive-status/{hiveId}")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
    @Operation(
            summary = "Atualizar status da colmeia",
            description = "Permite que técnicos atualizem o status de uma colmeia (ACTIVE/INACTIVE). Requer papel de TECHNICIAN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Status da colmeia atualizado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 400,
                                                "error": "Validation Error",
                                                "message": {
                                                    "hiveStatus": "O status da colmeia é obrigatório"
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não tem permissão de técnico",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "Acesso negado."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Colmeia não encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "Hive não encontrada"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<Void> updateHiveStatus(
            @Parameter(description = "ID da colmeia", required = true)
            @PathVariable UUID hiveId,
            @Valid @RequestBody UpdateHiveStatusRequest request
    ) {
        hiveUseCase.updateHiveStatus(hiveId, request.hiveStatus());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("technician/hives/{hiveId}")
    @PreAuthorize("hasAuthority('ROLE_TECHNICIAN')")
    @Operation(
            summary = "Excluir colmeia",
            description = "Permite que técnicos excluam uma colmeia e restaurem o contador de colmeias disponíveis do usuário. Requer papel de TECHNICIAN."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Colmeia excluída com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token de acesso inválido ou expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Token inválido ou expirado"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não tem permissão de técnico",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "Acesso negado."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Colmeia não encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "timestamp": "2024-01-01T10:00:00",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "Hive não encontrada"
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<Void> deleteHiveById(
            @Parameter(description = "ID da colmeia", required = true)
            @PathVariable UUID hiveId
    ) {
        hiveUseCase.deleteHive(hiveId);

        return ResponseEntity.noContent().build();
    }
}
