package com.course.controller;

import com.course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseController {

    @Autowired
    CourseService courseService;

    @GetMapping("/api/courses")
    public String getCourses() {
        return "Liste des cours IA!!";
    }

    @GetMapping("/api/generate")
    public String generateCourseBook() {


        courseService.generateCourseBook("Apprendre l'anglais pour les débutants");

        return "Génération réussie!!";


    }

}
