package com.aiminutes.service;

import com.aiminutes.dto.SummarizeResponse;

public interface SummarizationService {

    /**
     * Generates polished, formal Markdown meeting minutes from a raw transcript.
     *
     * @param text             the raw transcript text
     * @param originalLanguage ISO language code of the source transcript
     * @param targetLanguage   ISO language code the minutes should be produced in
     * @return the generated minutes
     */
    SummarizeResponse summarize(String text, String originalLanguage, String targetLanguage);
}
