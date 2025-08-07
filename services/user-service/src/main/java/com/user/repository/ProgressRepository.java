package com.user.repository;

import com.user.model.Progress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    /** Récupérer tout le suivi d’un utilisateur donné */
    List<Progress> findByUserId(Long userId);

    /** Facultatif : suivi d’un utilisateur pour un livre précis */
    List<Progress> findByUserIdAndBookId(Long userId, Long bookId);
}
