package com.course.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "subchapters", schema = "courses")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Subchapter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(nullable = false)
    private int idx;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chapter_id")
    private Chapter chapter;
    @OneToMany(mappedBy = "subchapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    public String title() { return title; }
    public List<Section> sections() { return sections; }
    public void setChapter(Chapter c) { this.chapter = c; }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public void addSection(Section s) {
        sections.add(s);
        s.setSubchapter(this);
    }

    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) {
        this.sections = sections;
        for (Section s : sections) {
            s.setSubchapter(this);
        }
    }

    public Long getId() {
        return id;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public String getTitle() {
        return title;
    }
}