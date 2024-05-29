package com.yf.exam.core.api.dto;


import lombok.Data;

import java.util.List;

@Data
public class BaseGeneDTO extends BaseDTO{

    private String type;
    private String subject;

    private String level;

    private String number;

    private String[] repoIds;
}
