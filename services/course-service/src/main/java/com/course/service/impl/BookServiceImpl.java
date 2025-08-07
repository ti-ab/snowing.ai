package com.course.service.impl;

import com.course.dto.BookDTO;
import com.course.dto.ChapterDTO;
import com.course.dto.SectionDTO;
import com.course.dto.SubchapterDTO;
import com.course.model.Book;
import com.course.repository.BookRepository;
import com.course.service.BookService;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class BookServiceImpl implements BookService {

    @Autowired
    BookRepository bookRepository;

    public List<BookDTO> fullTree() {
        return bookRepository.findAllWithTree().stream().map(b -> new BookDTO(
                b.getId(),
                b.getTitle(),
                b.getAuthors(),
                b.getChapters().stream().map(c -> new ChapterDTO(
                        c.getId(), c.getIdx(), c.getTitle(),
                        c.getSubchapters().stream().map(sc -> new SubchapterDTO(
                                sc.getId(), sc.getIdx(), sc.getTitle(),
                                sc.getSections().stream()
                                        .map(s -> new SectionDTO(s.getId(), s.getIdx(), s.getTitle(), s.getContent()))
                                        .toList()
                        )).toList()
                )).toList()
        )).toList();
    }

    public List<BookDTO> fullTreeNotSimultaneous() {

        // 1) première requête : uniquement les books
        List<Book> books = bookRepository.findAll();          // SELECT * FROM books

        // 2-3-4) on initialise chaque niveau paresseux en blocs
        books.forEach(b -> {
            Hibernate.initialize(b.getChapters());                  // req. chapters
            b.getChapters().forEach(c -> {
                Hibernate.initialize(c.getSubchapters());           // req. subchapters
                c.getSubchapters().forEach(sc ->
                        Hibernate.initialize(sc.getSections()));        // req. sections
            });
        });

        // 5) mapping manuel vers les DTO (Solution A)
        return books.stream().map(b -> new BookDTO(
                b.getId(),
                b.getTitle(),
                b.getAuthors(),
                b.getChapters().stream().map(c -> new ChapterDTO(
                        c.getId(), c.getIdx(), c.getTitle(),
                        c.getSubchapters().stream().map(sc -> new SubchapterDTO(
                                sc.getId(), sc.getIdx(), sc.getTitle(),
                                sc.getSections().stream()
                                        .map(s -> new SectionDTO(
                                                s.getId(), s.getIdx(),
                                                s.getTitle(), s.getContent()))
                                        .toList()
                        )).toList()
                )).toList()
        )).toList();
    }



}
