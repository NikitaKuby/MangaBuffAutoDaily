package ru.finwax.mangabuffjob.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Getter
@Setter
public class UserAgentService {
    private final Map<Long, String> userAgents;

    public UserAgentService(Map<Long, String> userAgents) {
        this.userAgents = userAgents;
    }


}