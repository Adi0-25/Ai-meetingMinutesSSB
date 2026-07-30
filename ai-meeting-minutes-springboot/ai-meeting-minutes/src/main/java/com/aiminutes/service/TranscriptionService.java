package com.aiminutes.service;

import com.aiminutes.dto.TranscriptionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface TranscriptionService {

    /**
     * Transcribes an uploaded audio file into text.
     *
     * @param audioFile the uploaded meeting audio
     * @param modelHint an optional hint about which ASR model/quality tier to use
     * @return the transcription result
     */
    TranscriptionResponse transcribe(MultipartFile audioFile, String modelHint);
}
