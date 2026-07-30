package com.aiminutes.service;

import com.aiminutes.dto.MeetingDetailView;
import com.aiminutes.dto.MeetingRequest;
import com.aiminutes.dto.MeetingSummaryView;
import com.aiminutes.exception.ResourceNotFoundException;
import com.aiminutes.model.Meeting;
import com.aiminutes.repository.MeetingRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;

    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public MeetingDetailView save(MeetingRequest request) {
        Meeting meeting = new Meeting(
                request.getTitle(),
                request.getTranscript(),
                request.getMinutesMarkdown(),
                request.getLanguage()
        );
        Meeting saved = meetingRepository.save(meeting);
        return toDetailView(saved);
    }

    public List<MeetingSummaryView> findAll() {
        return meetingRepository.findAll().stream()
                .sorted(Comparator.comparing(Meeting::getCreatedAt).reversed())
                .map(m -> new MeetingSummaryView(m.getId(), m.getTitle(), m.getLanguage(), m.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public MeetingDetailView findById(Long id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting not found with id " + id));
        return toDetailView(meeting);
    }

    public void deleteById(Long id) {
        if (!meetingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Meeting not found with id " + id);
        }
        meetingRepository.deleteById(id);
    }

    private MeetingDetailView toDetailView(Meeting m) {
        return new MeetingDetailView(
                m.getId(), m.getTitle(), m.getTranscript(), m.getMinutesMarkdown(), m.getLanguage(), m.getCreatedAt()
        );
    }
}
