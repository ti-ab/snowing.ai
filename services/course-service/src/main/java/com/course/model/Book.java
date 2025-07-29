package com.course.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books", schema = "courses")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String authors;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters = new ArrayList<>();

    protected Book() {/* JPA */}
    public Book(String title, String authors) { this.title = title; this.authors = authors; }

    public void addChapter(Chapter c) { c.setBook(this); chapters.add(c); }
    public void setCreatedAt(LocalDateTime at) { this.createdAt = at; }
}