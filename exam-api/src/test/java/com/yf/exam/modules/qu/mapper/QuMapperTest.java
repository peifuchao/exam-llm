package com.yf.exam.modules.qu.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuMapperTest {

    @Autowired
    private QuMapper quMapper;
    @Test
    void queryQu() {
        System.out.println(quMapper.queryQu("五个答案中哪个是最好的类比？女儿对于父亲相当于侄女对于"));
    }

    @Test
    void insertQu() {


    }
}