package com.aiminutes.service.impl;

import com.aiminutes.dto.TranscriptSegment;
import com.aiminutes.dto.TranscriptionResponse;
import com.aiminutes.exception.TranscriptionException;
import com.aiminutes.service.TranscriptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Transcribes meeting audio using OpenAI's Whisper API (the hosted equivalent of the
 * faster-whisper model used in the original Python prototype).
 * <p>
 * Requires the {@code OPENAI_API_KEY} environment variable to be set. If it is not set,
 * a clear {@link TranscriptionException} is thrown instead of a raw network error.
 */
@Service
public class OpenAiTranscriptionService implements TranscriptionService {

    private static final String WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.model.whisper:whisper-1}")
    private String whisperModel;

    public OpenAiTranscriptionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public TranscriptionResponse transcribe(MultipartFile audioFile, String modelHint) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new TranscriptionException(
                    "No OpenAI API key configured. Set the OPENAI_API_KEY environment variable to enable transcription."
            );
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);

            byte[] bytes = audioFile.getBytes();
            String originalFilename = audioFile.getOriginalFilename() != null
                    ? audioFile.getOriginalFilename() : "audio.mp3";

            ByteArrayResource fileResource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            body.add("model", whisperModel);
            body.add("response_format", "verbose_json");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(WHISPER_URL, requestEntity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root.path("text").asText("");
            String detectedLanguage = root.path("language").asText("en");

            List<TranscriptSegment> segments = new ArrayList<>();
            JsonNode segmentsNode = root.path("segments");
            if (segmentsNode.isArray()) {
                for (JsonNode seg : segmentsNode) {
                    segments.add(new TranscriptSegment(
                            seg.path("start").asDouble(),
                            seg.path("end").asDouble(),
                            seg.path("text").asText("").trim()
                    ));
                }
            }

            TranscriptionResponse result = new TranscriptionResponse();
            result.setText(text.trim());
            result.setLanguage("en");
            result.setOriginalAudioLanguage(detectedLanguage);
            result.setSegments(segments);
            return result;

        } catch (HttpClientErrorException e) {
            throw new TranscriptionException("OpenAI transcription request failed: " + e.getStatusCode(), e);
        } catch (IOException e) {
            throw new TranscriptionException("Could not read the uploaded audio file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TranscriptionException("Transcription failed: " + e.getMessage(), e);
        }
    }
}
