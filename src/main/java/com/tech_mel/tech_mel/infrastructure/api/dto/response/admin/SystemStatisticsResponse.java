package com.tech_mel.tech_mel.infrastructure.api.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatisticsResponse {
    
    private long totalUsers;
    
    private long activeUsers;
    
    private long inactiveUsers;
    
    private long adminUsers;
    
    private long technicianUsers;
    
    private long commonUsers;
    
    private long newUsersLastMonth;
    
    private long lockedUsers;
    
    private long unverifiedUsers;
    
    private Map<String, Long> usersByRole;
    
    private Map<String, Long> usersByStatus;
    
    private Map<String, Long> usersByAuthProvider;
    
    private Map<String, Long> registrationsByMonth;
    
    private LocalDateTime generatedAt;
}
