package com.course.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "chapters", schema = "courses")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Chapter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    private String title;


    @Column(nullable = false)
    private int idx;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "book_id")
    private Book book;
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subchapter> subchapters = new ArrayList<>();

    public String title() { return title; }
    public List<Subchapter> subchapters() { return subchapters; }
    public void setBook(Book b) { this.book = b; }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public void addSubchapter(Subchapter sc) {
        subchapters.add(sc);
        sc.setChapter(this);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // renommer votre getter
    public List<Subchapter> getSubchapters() { return subchapters; }
    // ajouter le setter
    public void setSubchapters(List<Subchapter> subchapters) {
        this.subchapters = subchapters;
        // relier les deux côtés de la relation
        for (Subchapter sc : subchapters) {
            sc.setChapter(this);
        }
    }

}