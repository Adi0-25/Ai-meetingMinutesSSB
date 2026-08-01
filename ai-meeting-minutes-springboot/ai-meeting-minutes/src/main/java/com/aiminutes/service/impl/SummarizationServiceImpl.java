package com.aiminutes.service.impl;

import com.aiminutes.dto.SummarizeResponse;
import com.aiminutes.service.OpenAiKeyPool;
import com.aiminutes.service.SummarizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SummarizationServiceImpl implements SummarizationService {

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "is", "are", "was", "were", "to", "of", "in", "on",
            "for", "with", "that", "this", "it", "as", "be", "we", "i", "you", "at", "by", "from", "so",
            "there", "their", "have", "has", "had", "not", "just", "about", "which", "they", "them"
    );

    private final RestTemplate restTemplate;
    private final OpenAiKeyPool keyPool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.model.chat:gpt-4o-mini}")
    private String chatModel;

    public SummarizationServiceImpl(RestTemplate restTemplate, OpenAiKeyPool keyPool) {
        this.restTemplate = restTemplate;
        this.keyPool = keyPool;
    }

    @Override
    public SummarizeResponse summarize(String text, String originalLanguage, String targetLanguage) {
        if (text == null || text.isBlank()) {
            return new SummarizeResponse("", targetLanguage);
        }

        if (keyPool.hasKeys()) {
            try {
                return summarizeWithOpenAi(text, targetLanguage);
            } catch (Exception e) {
                System.err.println("OpenAI summarization failed, falling back to the local summarizer: " + e.getMessage());
            }
        }

        return summarizeLocally(text, targetLanguage);
    }

    private SummarizeResponse summarizeWithOpenAi(String text, String targetLanguage) {
        String languageInstruction = "en".equalsIgnoreCase(targetLanguage)
                ? ""
                : " Respond entirely in the language with ISO code '" + targetLanguage + "'.";

        String userPrompt = "Please generate professional Meeting Minutes from the following transcript. "
                + "Format your response in Markdown with the following sections:\n\n"
                + "# Meeting Minutes\n## Executive Summary\n## Key Discussion Points\n## Decisions Made\n## Action Items\n\n"
                + languageInstruction + "\n\nTranscript:\n" + text;

        Map<String, Object> body = new HashMap<>();
        body.put("model", chatModel);
        body.put("temperature", 0.4);
        body.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are a highly professional Executive Assistant. Your task is to read the provided "
                                + "meeting transcript and produce polished, formal Meeting Minutes."),
                Map.of("role", "user", "content", userPrompt)
        ));

        List<String> keys = keyPool.getKeys();
        Exception lastFailure = null;

        for (int i = 0; i < keys.size(); i++) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(keys.get(i));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(CHAT_URL, requestEntity, String.class);
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0).path("message").path("content").asText("");
                return new SummarizeResponse(content.trim(), targetLanguage);
            } catch (HttpClientErrorException e) {
                lastFailure = e;
                boolean canRetryNextKey = keyPool.isKeyLevelFailure(e) && i < keys.size() - 1;
                if (!canRetryNextKey) {
                    throw new RuntimeException("OpenAI summarization request failed: " + e.getStatusCode(), e);
                }
                System.err.println("OpenAI key #" + (i + 1) + " failed with " + e.getStatusCode()
                        + ", trying next key for summarization...");
            } catch (Exception e) {
                throw new RuntimeException("Could not parse the OpenAI response", e);
            }
        }

        throw new RuntimeException("All configured OpenAI API keys failed for summarization.", lastFailure);
    }

    private SummarizeResponse summarizeLocally(String text, String targetLanguage) {
        List<String> sentences = splitIntoSentences(text);

        List<String> decisions = filterByKeywords(sentences,
                "decid", "agreed", "approve", "resolved", "concluded", "finalized");

        List<String> actionItems = filterByKeywords(sentences,
                "will ", "action item", "todo", "to-do", "assigned", "should ", "needs to",
                "deadline", "follow up", "follow-up", "responsible for");

        List<String> keyPoints = rankSentencesByKeywordFrequency(sentences, 5);

        String executiveSummary = keyPoints.isEmpty()
                ? "No substantial discussion content was detected in the transcript."
                : String.join(" ", keyPoints.subList(0, Math.min(2, keyPoints.size())));

        StringBuilder md = new StringBuilder();
        md.append("# Meeting Minutes\n\n");
        md.append("## Executive Summary\n").append(executiveSummary).append("\n\n");

        md.append("## Key Discussion Points\n");
        appendBulletsOrFallback(md, keyPoints, "No key points detected.");
        md.append("\n");

        md.append("## Decisions Made\n");
        appendBulletsOrFallback(md, decisions, "No explicit decisions detected in the transcript.");
        md.append("\n");

        md.append("## Action Items\n");
        appendBulletsOrFallback(md, actionItems, "No explicit action items detected in the transcript.");
        md.append("\n");

        md.append("_Generated locally using a keyword-based summarizer (no OpenAI API key configured)._\n");

        return new SummarizeResponse(md.toString(), targetLanguage);
    }

    private void appendBulletsOrFallback(StringBuilder md, List<String> items, String fallbackMessage) {
        if (items.isEmpty()) {
            md.append("- ").append(fallbackMessage).append("\n");
            return;
        }
        for (String item : items) {
            md.append("- ").append(item).append("\n");
        }
    }

    private List<String> splitIntoSentences(String text) {
        String[] raw = text.split("(?<=[.?!])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String s : raw) {
            String trimmed = s.trim();
            if (trimmed.length() > 15) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private List<String> filterByKeywords(List<String> sentences, String... keywords) {
        List<String> matches = new ArrayList<>();
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase(Locale.ROOT);
            for (String kw : keywords) {
                if (lower.contains(kw)) {
                    matches.add(sentence);
                    break;
                }
            }
            if (matches.size() >= 6) {
                break;
            }
        }
        return matches;
    }

    private List<String> rankSentencesByKeywordFrequency(List<String> sentences, int topN) {
        Map<String, Integer> wordFreq = new HashMap<>();
        Pattern wordPattern = Pattern.compile("[a-zA-Z]+");

        for (String sentence : sentences) {
            Matcher m = wordPattern.matcher(sentence.toLowerCase(Locale.ROOT));
            while (m.find()) {
                String word = m.group();
                if (!STOP_WORDS.contains(word) && word.length() > 3) {
                    wordFreq.merge(word, 1, Integer::sum);
                }
            }
        }

        Map<String, Double> scores = new LinkedHashMap<>();
        for (String sentence : sentences) {
            Matcher m = wordPattern.matcher(sentence.toLowerCase(Locale.ROOT));
            double score = 0;
            int count = 0;
            while (m.find()) {
                String word = m.group();
                score += wordFreq.getOrDefault(word, 0);
                count++;
            }
            scores.put(sentence, count == 0 ? 0 : score / count);
        }

        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
