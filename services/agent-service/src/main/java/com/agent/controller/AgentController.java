package com.agent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {
    @GetMapping("/api/agents")
    public String getAgents() {
        return "Liste des agents IA!!";
    }
}
