package com.course.service;

import com.course.model.Book;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public interface BookService {

    List<Book> fullTree();


}