package com.aiminutes.service;

import com.aiminutes.dto.SummarizeResponse;
import com.aiminutes.service.impl.SummarizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummarizationServiceImplTest {

    private SummarizationServiceImpl summarizationService;

    @BeforeEach
    void setUp() {
        summarizationService = new SummarizationServiceImpl(new RestTemplate());
        // No OpenAI API key configured -> exercises the offline fallback summarizer.
        ReflectionTestUtils.setField(summarizationService, "apiKey", "");
    }

    @Test
    void returnsEmptyResultForBlankTranscript() {
        SummarizeResponse response = summarizationService.summarize("   ", "en", "en");
        assertEquals("", response.getProfessionalMinutes());
    }

    @Test
    void generatesStructuredMarkdownWithoutAnApiKey() {
        String transcript = "Sarah: Good morning everyone, let's start the Q4 review. "
                + "Mike: Revenue is up 25 percent compared to last quarter. "
                + "Jennifer: We decided to finalize the budget by Friday. "
                + "Mike: I will send the updated numbers to everyone by tomorrow. "
                + "Sarah: Great, let's follow up next week.";

        SummarizeResponse response = summarizationService.summarize(transcript, "en", "en");
        String minutes = response.getProfessionalMinutes();

        assertTrue(minutes.contains("# Meeting Minutes"));
        assertTrue(minutes.contains("## Executive Summary"));
        assertTrue(minutes.contains("## Key Discussion Points"));
        assertTrue(minutes.contains("## Decisions Made"));
        assertTrue(minutes.contains("## Action Items"));
        assertEquals("en", response.getLanguage());
    }
}
