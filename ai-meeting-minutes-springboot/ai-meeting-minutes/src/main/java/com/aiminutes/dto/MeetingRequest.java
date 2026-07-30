package com.aiminutes.dto;

import jakarta.validation.constraints.NotBlank;

public class MeetingRequest {

    @NotBlank(message = "title must not be blank")
    private String title;

    @NotBlank(message = "transcript must not be blank")
    private String transcript;

    private String minutesMarkdown;

    private String language;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getMinutesMarkdown() {
        return minutesMarkdown;
    }

    public void setMinutesMarkdown(String minutesMarkdown) {
        this.minutesMarkdown = minutesMarkdown;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
