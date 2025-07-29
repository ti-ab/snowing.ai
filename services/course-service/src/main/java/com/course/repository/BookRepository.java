package com.course.repository;

import com.course.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/* ──────────────────────────────────────────────────────────────────
   Spring‑Data repository (package‑private, même fichier pour la démo)
   ─────────────────────────────────────────────────────────────────*/
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {}