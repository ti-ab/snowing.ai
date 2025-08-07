package com.course.service;

import com.course.dto.BookDTO;

import java.util.List;

public interface BookService {

    List<BookDTO> fullTree();

    List<BookDTO> fullTreeNotSimultaneous();


}