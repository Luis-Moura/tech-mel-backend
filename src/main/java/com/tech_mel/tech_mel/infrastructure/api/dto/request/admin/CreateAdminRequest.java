package com.tech_mel.tech_mel.infrastructure.api.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAdminRequest {
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter um formato válido")
    private String email;
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;
    
    @Size(min = 8, max = 50, message = "Senha deve ter entre 8 e 50 caracteres")
    private String password; // Opcional - se não fornecida, será gerada automaticamente
    
    private Integer availableHives = 50; // Padrão para admins
}
