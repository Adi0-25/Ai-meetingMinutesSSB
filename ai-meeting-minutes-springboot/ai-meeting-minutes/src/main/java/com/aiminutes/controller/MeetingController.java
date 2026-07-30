package com.aiminutes.controller;

import com.aiminutes.dto.MeetingDetailView;
import com.aiminutes.dto.MeetingRequest;
import com.aiminutes.dto.MeetingSummaryView;
import com.aiminutes.dto.SummarizeRequest;
import com.aiminutes.dto.SummarizeResponse;
import com.aiminutes.dto.TranscriptionResponse;
import com.aiminutes.service.MeetingService;
import com.aiminutes.service.SummarizationService;
import com.aiminutes.service.TranscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Tag(name = "Meeting Minutes", description = "Transcribe audio, generate AI meeting minutes, and manage meeting history")
public class MeetingController {

    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = Set.of(
            ".mp3", ".wav", ".m4a", ".ogg", ".flac", ".aac", ".webm"
    );

    private final TranscriptionService transcriptionService;
    private final SummarizationService summarizationService;
    private final MeetingService meetingService;

    public MeetingController(TranscriptionService transcriptionService,
                              SummarizationService summarizationService,
                              MeetingService meetingService) {
        this.transcriptionService = transcriptionService;
        this.summarizationService = summarizationService;
        this.meetingService = meetingService;
    }

    @Operation(summary = "Transcribe an uploaded audio file into text")
    @PostMapping(value = "/transcribe", consumes = "multipart/form-data")
    public ResponseEntity<?> transcribe(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "model", required = false, defaultValue = "base") String model) {

        if (audio.isEmpty()) {
            return ResponseEntity.badRequest().body(errorBody("No audio file provided"));
        }

        String filename = audio.getOriginalFilename() == null ? "" : audio.getOriginalFilename().toLowerCase();
        boolean allowed = ALLOWED_AUDIO_EXTENSIONS.stream().anyMatch(filename::endsWith);
        if (!allowed) {
            return ResponseEntity.badRequest().body(errorBody("Unsupported file type"));
        }

        TranscriptionResponse response = transcriptionService.transcribe(audio, model);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Generate professional meeting minutes from a transcript")
    @PostMapping("/summarize")
    public ResponseEntity<SummarizeResponse> summarize(@Valid @RequestBody SummarizeRequest request) {
        SummarizeResponse response = summarizationService.summarize(
                request.getText(), request.getLanguage(), request.getTargetLang()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Save a meeting (transcript + generated minutes) to history")
    @PostMapping("/meetings")
    public ResponseEntity<MeetingDetailView> saveMeeting(@Valid @RequestBody MeetingRequest request) {
        MeetingDetailView saved = meetingService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "List all previously saved meetings")
    @GetMapping("/meetings")
    public ResponseEntity<List<MeetingSummaryView>> listMeetings() {
        return ResponseEntity.ok(meetingService.findAll());
    }

    @Operation(summary = "Get a single saved meeting by id")
    @GetMapping("/meetings/{id}")
    public ResponseEntity<MeetingDetailView> getMeeting(@PathVariable Long id) {
        return ResponseEntity.ok(meetingService.findById(id));
    }

    @Operation(summary = "Delete a saved meeting")
    @DeleteMapping("/meetings/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable Long id) {
        meetingService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, String> errorBody(String message) {
        return Map.of("error", message);
    }
}
