package com.course.service;

import com.course.model.Book;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Interface publique du service de génération de livres de cours.
 */
public interface CourseService {

    /**
     * Génère un livre de cours complet (titre, chapitres, contenu, etc.) à partir
     * d'une description simple (ex: "Apprendre l'anglais pour les nuls").
     *
     * @param bookDescription résumé ou thème du livre de formation à générer
     * @return un objet {@link Book} contenant l'ensemble du contenu structuré
     */
    Book generateCourseBook(String bookDescription);

}