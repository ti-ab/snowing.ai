package com.course.service.impl;

import com.course.dto.BookDTO;
import com.course.dto.ChapterDTO;
import com.course.dto.SectionDTO;
import com.course.dto.SubchapterDTO;
import com.course.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class BookServiceImpl {

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


}
