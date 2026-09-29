package com.aquarium.gateway.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "aquarium.gateway.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private int defaultLimit = 300;

    private Duration defaultWindow = Duration.ofMinutes(1);

    private int authLimit = 20;

    private Duration authWindow = Duration.ofMinutes(1);

    private List<String> authPaths = new ArrayList<>();

    private boolean trustForwardedFor = false;
}
