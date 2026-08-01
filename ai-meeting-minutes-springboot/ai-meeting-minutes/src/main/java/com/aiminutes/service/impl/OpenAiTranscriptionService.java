package com.aiminutes.service.impl;

import com.aiminutes.dto.TranscriptSegment;
import com.aiminutes.dto.TranscriptionResponse;
import com.aiminutes.exception.TranscriptionException;
import com.aiminutes.service.OpenAiKeyPool;
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

@Service
public class OpenAiTranscriptionService implements TranscriptionService {

    private static final String WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final RestTemplate restTemplate;
    private final OpenAiKeyPool keyPool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.model.whisper:whisper-1}")
    private String whisperModel;

    public OpenAiTranscriptionService(RestTemplate restTemplate, OpenAiKeyPool keyPool) {
        this.restTemplate = restTemplate;
        this.keyPool = keyPool;
    }

    @Override
    public TranscriptionResponse transcribe(MultipartFile audioFile, String modelHint) {
        if (!keyPool.hasKeys()) {
            throw new TranscriptionException(
                    "No OpenAI API key configured. Set the OPENAI_API_KEY (or OPENAI_API_KEYS for multiple, "
                            + "comma-separated) environment variable to enable transcription."
            );
        }

        byte[] bytes;
        String originalFilename = audioFile.getOriginalFilename() != null
                ? audioFile.getOriginalFilename() : "audio.mp3";
        try {
            bytes = audioFile.getBytes();
        } catch (IOException e) {
            throw new TranscriptionException("Could not read the uploaded audio file: " + e.getMessage(), e);
        }

        List<String> keys = keyPool.getKeys();
        Exception lastFailure = null;

        for (int i = 0; i < keys.size(); i++) {
            String apiKey = keys.get(i);
            try {
                return callWhisper(apiKey, bytes, originalFilename);
            } catch (HttpClientErrorException e) {
                lastFailure = e;
                boolean canRetryNextKey = keyPool.isKeyLevelFailure(e) && i < keys.size() - 1;
                if (!canRetryNextKey) {
                    throw new TranscriptionException("OpenAI transcription request failed: " + e.getStatusCode(), e);
                }
                System.err.println("OpenAI key #" + (i + 1) + " failed with " + e.getStatusCode()
                        + ", trying next key for transcription...");
            } catch (Exception e) {
                throw new TranscriptionException("Transcription failed: " + e.getMessage(), e);
            }
        }

        throw new TranscriptionException("All configured OpenAI API keys failed for transcription.", lastFailure);
    }

    private TranscriptionResponse callWhisper(String apiKey, byte[] bytes, String originalFilename) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);

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
    }
}
