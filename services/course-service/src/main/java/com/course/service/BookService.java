package com.course.service;

import com.course.dto.BookDTO;

import java.util.List;
import java.util.Optional;

public interface BookService {

    List<BookDTO> fullTree();

    List<BookDTO> fullTreeNotSimultaneous();

    List<BookDTO> listBooksOnly();

    BookDTO fullTreeById(Long id);


    }