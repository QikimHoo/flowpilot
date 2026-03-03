package com.bank.progress.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveNodeRequest {
    private String newParentId;
    private Integer newOrder;
}
