package com.aiminutes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SummarizeResponse {

    @JsonProperty("professional_minutes")
    private String professionalMinutes;

    private String language;

    public SummarizeResponse() {
    }

    public SummarizeResponse(String professionalMinutes, String language) {
        this.professionalMinutes = professionalMinutes;
        this.language = language;
    }

    public String getProfessionalMinutes() {
        return professionalMinutes;
    }

    public void setProfessionalMinutes(String professionalMinutes) {
        this.professionalMinutes = professionalMinutes;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
