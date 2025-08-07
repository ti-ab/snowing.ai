package com.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "progress", schema = "users")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Progress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiants de référence (simples valeurs, car les entités se trouvent dans d’autres micro-services) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "subchapter_id")
    private Long subchapterId;

    @Column(name = "section_id")
    private Long sectionId;

    /** Données de suivi */
    private Double sectionRating;         // note de la section (ex. de 0 à 5)
    private Long   sectionTime;           // temps passé en secondes

    private LocalDateTime startTimestamp; // début de la section
    private LocalDateTime endTimestamp;   // fin de la section

    /* ====== Constructeurs ====== */
    protected Progress() {}                              // requis par JPA

    public Progress(Long userId, Long bookId) {          // constructeur minimal
        this.userId = userId;
        this.bookId = bookId;
    }

    /* ====== Getters / Setters (extraits) ====== */
    public Long getId()               { return id; }
    public Long getUserId()           { return userId; }
    public void setUserId(Long id)    { this.userId = id; }

    public Long getBookId()           { return bookId; }
    public void setBookId(Long id)    { this.bookId = id; }

    public Long getChapterId()        { return chapterId; }
    public void setChapterId(Long id) { this.chapterId = id; }

    public Long getSubchapterId()           { return subchapterId; }
    public void setSubchapterId(Long id)    { this.subchapterId = id; }

    public Long getSectionId()        { return sectionId; }
    public void setSectionId(Long id) { this.sectionId = id; }

    public Double getSectionRating()               { return sectionRating; }
    public void   setSectionRating(Double rating)  { this.sectionRating = rating; }

    public Long getSectionTime()           { return sectionTime; }
    public void setSectionTime(Long time)  { this.sectionTime = time; }

    public LocalDateTime getStartTimestamp()                  { return startTimestamp; }
    public void          setStartTimestamp(LocalDateTime t)   { this.startTimestamp = t; }

    public LocalDateTime getEndTimestamp()                    { return endTimestamp; }
    public void          setEndTimestamp(LocalDateTime t)     { this.endTimestamp = t; }
}
