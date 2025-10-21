package com.ecetasci.hrmanagement.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    private boolean success;
    private int code;
    private String message;
    private LocalDateTime timestamp;
    private String path; // hangi endpoint çağrılmış
}
