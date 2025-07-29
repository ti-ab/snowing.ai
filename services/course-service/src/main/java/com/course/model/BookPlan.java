package com.course.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/* ─────────────────────────────────────────────────────────────
   DTO pour la première réponse GPT (plan du livre)
   ───────────────────────────────────────────────────────────*/
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookPlan(String mainTitle, String authors, List<ChapterPlan> chapters) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChapterPlan(String title, List<SubchapterPlan> subchapters) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubchapterPlan(String title, List<String> sections) {}
}