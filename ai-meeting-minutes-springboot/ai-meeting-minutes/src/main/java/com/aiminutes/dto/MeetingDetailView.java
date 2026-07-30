package com.aiminutes.dto;

import java.time.LocalDateTime;

public class MeetingDetailView {

    private Long id;
    private String title;
    private String transcript;
    private String minutesMarkdown;
    private String language;
    private LocalDateTime createdAt;

    public MeetingDetailView(Long id, String title, String transcript, String minutesMarkdown,
                              String language, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.transcript = transcript;
        this.minutesMarkdown = minutesMarkdown;
        this.language = language;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getMinutesMarkdown() {
        return minutesMarkdown;
    }

    public String getLanguage() {
        return language;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
