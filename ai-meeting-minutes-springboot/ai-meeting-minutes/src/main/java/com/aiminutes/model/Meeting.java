package com.aiminutes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String transcript;

    @Lob
    @Column(name = "minutes_markdown", columnDefinition = "CLOB")
    private String minutesMarkdown;

    private String language;

    private LocalDateTime createdAt;

    public Meeting() {
    }

    public Meeting(String title, String transcript, String minutesMarkdown, String language) {
        this.title = title;
        this.transcript = transcript;
        this.minutesMarkdown = minutesMarkdown;
        this.language = language;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
