package com.aiminutes.dto;

import java.time.LocalDateTime;

public class MeetingSummaryView {

    private Long id;
    private String title;
    private String language;
    private LocalDateTime createdAt;

    public MeetingSummaryView(Long id, String title, String language, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.language = language;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
