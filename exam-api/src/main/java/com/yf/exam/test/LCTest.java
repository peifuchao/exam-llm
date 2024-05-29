package com.yf.exam.test;


import com.yf.exam.llm.ChatAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LCTest {

//    @Value("sk-p0hanLa6b2bMrEyo6bE7D5D6405e424a92AfE9184a605dA9")
//    private String OPENAI_API_KEY;
//
//    private Assistant assistant;
//
//    interface Assistant {
//        String chat(String message);
//    }
//
//    @PostConstruct
//    public void init() {
//        TokenWindowChatMemory memory = TokenWindowChatMemory.withMaxTokens(1000, new OpenAiTokenizer("gpt-3.5-turbo"));
//
//        assistant = AiServices.builder(Assistant.class)
//                .chatLanguageModel(OpenAiChatModel.withApiKey(OPENAI_API_KEY))
//                .chatMemory(memory)
//                .build();
//    }
//
//    public String chat(String message) {
//        return assistant.chat(message);
//    }

    @Autowired
    ChatAgent agent;

    public void chat(String message){
        System.out.println(agent.chat(message));
    }


}
