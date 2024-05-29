package com.yf.exam.modules.qu.entity;


import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GQuestion {
    private String question;
    private String options;
    private String answer;
}
