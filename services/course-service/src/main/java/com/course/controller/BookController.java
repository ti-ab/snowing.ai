package com.course.controller;

import com.course.model.Book;
import com.course.repository.BookRepository;
import com.course.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookController {

    @Autowired
    BookService bookService;

    @GetMapping("/api/books")
    public List<Book> list() {
        return bookService.fullTree();   // ← entités, pas de DTO
    }
}

