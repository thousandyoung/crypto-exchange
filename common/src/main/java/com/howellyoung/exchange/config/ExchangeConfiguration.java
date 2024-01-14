package com.howellyoung.exchange.config;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "exchange.config") //only set default value of config in config class
public class ExchangeConfiguration {

    private int orderBookDepth = 50;
    private boolean debugMode = false;
    private String timeZone = ZoneId.systemDefault().getId(); //默认设置

    @Bean
    public ZoneId createZoneId() {
        return ZoneId.of(this.timeZone);
    }

    public int getOrderBookDepth() {
        return orderBookDepth;
    }

    public void setOrderBookDepth(int orderBookDepth) {
        this.orderBookDepth = orderBookDepth;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone.isEmpty() ? ZoneId.systemDefault().getId() : timeZone;
    }

}
