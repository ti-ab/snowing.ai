package com.course.dto;

import java.util.List;

public record SubchapterDTO(Long id, int idx, String title, List<SectionDTO> sections) {}
