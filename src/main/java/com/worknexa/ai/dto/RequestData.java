package com.worknexa.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestData {
    private String title;
    private BigDecimal amount;
    private String category;
    private String description;
    private boolean documentAttached;
}
