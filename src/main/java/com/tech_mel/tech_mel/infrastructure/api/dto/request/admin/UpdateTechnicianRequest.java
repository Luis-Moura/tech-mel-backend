package com.tech_mel.tech_mel.infrastructure.api.dto.request.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTechnicianRequest {
    
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;
    
    @Email(message = "Email deve ter um formato válido")
    private String email;
    
    @Min(value = 0, message = "Quantidade de colmeias disponíveis deve ser maior ou igual a 0")
    private Integer availableHives;
}
