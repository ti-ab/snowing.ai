package com.user.controller;

import com.user.model.Progress;
import com.user.repository.ProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {
    @Autowired
    private ProgressRepository progressRepo;

    @GetMapping("/{userId}")
    public List<Progress> list(@PathVariable Long userId) {
        return progressRepo.findByUserId(userId);
    }

    @PostMapping
    public Progress save(@RequestBody Progress p) {
        return progressRepo.save(p);
    }
}

