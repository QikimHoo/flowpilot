package com.bank.progress.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class WarningActionRequest {
    private String status;
    private String note;
    private Map<String, Object> attachment;
}
