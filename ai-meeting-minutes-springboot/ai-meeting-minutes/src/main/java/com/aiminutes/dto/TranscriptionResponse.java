package com.aiminutes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TranscriptionResponse {

    private String text;
    private String language;

    @JsonProperty("original_audio_language")
    private String originalAudioLanguage;

    private List<TranscriptSegment> segments;

    public TranscriptionResponse() {
    }

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

    public String getOriginalAudioLanguage() {
        return originalAudioLanguage;
    }

    public void setOriginalAudioLanguage(String originalAudioLanguage) {
        this.originalAudioLanguage = originalAudioLanguage;
    }

    public List<TranscriptSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<TranscriptSegment> segments) {
        this.segments = segments;
    }
}
