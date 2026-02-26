package com.example.demo.iface.rest;

import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {

    private final EventProcessingConfiguration eventProcessingConfiguration;

    @PostMapping("/reset-order-view")
    public void resetOrderView() {
        eventProcessingConfiguration.eventProcessor("order-group", TrackingEventProcessor.class)
            .ifPresent(tep -> {
                tep.shutDown();      // 1. 先停止
                tep.resetTokens();   // 2. 重置 Token 到最起點
                tep.start();         // 3. 重新啟動，開始重播
            });
    }
}