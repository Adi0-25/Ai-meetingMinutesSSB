package com.aiminutes.controller;

import com.aiminutes.dto.MeetingDetailView;
import com.aiminutes.dto.MeetingSummaryView;
import com.aiminutes.service.MeetingService;
import com.aiminutes.service.SummarizationService;
import com.aiminutes.service.TranscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TranscriptionService transcriptionService;

    @MockBean
    private SummarizationService summarizationService;

    @MockBean
    private MeetingService meetingService;

    @Test
    void listMeetingsReturnsSavedHistory() throws Exception {
        when(meetingService.findAll()).thenReturn(List.of(
                new MeetingSummaryView(1L, "Q4 Review", "en", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/meetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Q4 Review"));
    }

    @Test
    void saveMeetingRejectsBlankTitle() throws Exception {
        String invalidPayload = objectMapper.writeValueAsString(
                java.util.Map.of("title", "", "transcript", "some transcript")
        );

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveMeetingPersistsValidPayload() throws Exception {
        when(meetingService.save(any())).thenReturn(
                new MeetingDetailView(1L, "Q4 Review", "transcript text", "# Meeting Minutes", "en", LocalDateTime.now())
        );

        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("title", "Q4 Review", "transcript", "transcript text")
        );

        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Q4 Review"));
    }
}
