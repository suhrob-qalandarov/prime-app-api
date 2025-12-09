package org.exp.primeapp.models.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String action; // CREATE, UPDATE, DELETE, etc.
    
    private String description; // Activity description
    
    private String performedBy; // User who performed the action
    
    private String details; // Additional details as JSON string or text
}

