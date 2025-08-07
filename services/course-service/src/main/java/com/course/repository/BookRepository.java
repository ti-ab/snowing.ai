package com.course.repository;

import com.course.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/* ──────────────────────────────────────────────────────────────────
   Spring‑Data repository (package‑private, même fichier pour la démo)
   ─────────────────────────────────────────────────────────────────*/
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
         select distinct b
         from Book b
           left join fetch b.chapters c
           left join fetch c.subchapters s
           left join fetch s.sections
         """)
    List<Book> findAllWithTree();

}