package com.course.dto;

import java.util.List;
import java.util.stream.Collectors;

public record BookDTO(Long id, String title, String authors, List<ChapterDTO> chapters) {}
