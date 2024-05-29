package com.yf.exam.test;


import com.yf.exam.ExamApplication;
import com.yf.exam.modules.qu.entity.Qu;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
class LCTestTest {

    @Autowired
    private LCTest lctest;
    @Test
    void   test1() {

        lctest.chat("生成题目：\n题目类型：单选题\n科目:工商管理\n难度：较难\n数量:2\n");

    }

    @Test
    void test2(){
        Qu qu = new Qu();

        System.out.println(qu);
    }
}