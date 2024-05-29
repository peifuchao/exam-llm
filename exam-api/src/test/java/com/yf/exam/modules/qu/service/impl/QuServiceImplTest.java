package com.yf.exam.modules.qu.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuServiceImplTest {

    @Autowired
    private QuServiceImpl quService;
    @Test
    void generateAnalysis() {

        System.out.println(quService.generateAnalysis("1587622644134285314"));

    }

    @Test
    void generateQuestion() {

//        System.out.println(quService.generateQuestion("2","大学物理","2",2));
    }
}