package com.course.dto;

import java.util.List;

public record ChapterDTO(Long id, int idx, String title, List<SubchapterDTO> subchapters) {}
