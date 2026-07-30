package com.aiminutes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class SummarizeRequest {

    @NotBlank(message = "text must not be blank")
    private String text;

    private String language = "en";

    @JsonProperty("target_lang")
    private String targetLang = "en";

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public void setTargetLang(String targetLang) {
        this.targetLang = targetLang;
    }
}
