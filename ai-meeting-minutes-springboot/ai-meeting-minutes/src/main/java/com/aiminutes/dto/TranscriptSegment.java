package com.aiminutes.dto;

public class TranscriptSegment {

    private double start;
    private double end;
    private String text;

    public TranscriptSegment() {
    }

    public TranscriptSegment(double start, double end, String text) {
        this.start = start;
        this.end = end;
        this.text = text;
    }

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public double getEnd() {
        return end;
    }

    public void setEnd(double end) {
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
