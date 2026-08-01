package com.aiminutes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class OpenAiKeyPool {

    private final List<String> keys;

    public OpenAiKeyPool(@Value("${openai.api-keys:}") String rawKeys) {
        List<String> parsed = new ArrayList<>();
        if (rawKeys != null && !rawKeys.isBlank()) {
            for (String key : rawKeys.split(",")) {
                String trimmed = key.trim();
                if (!trimmed.isEmpty()) {
                    parsed.add(trimmed);
                }
            }
        }
        this.keys = Collections.unmodifiableList(parsed);
    }

    public boolean hasKeys() {
        return !keys.isEmpty();
    }

    public List<String> getKeys() {
        return keys;
    }

    public boolean isKeyLevelFailure(Exception e) {
        if (e instanceof HttpClientErrorException httpEx) {
            HttpStatus status = HttpStatus.resolve(httpEx.getStatusCode().value());
            return status == HttpStatus.UNAUTHORIZED
                    || status == HttpStatus.FORBIDDEN
                    || status == HttpStatus.TOO_MANY_REQUESTS;
        }
        return false;
    }
}
