package com.sigeclin.maestras.controller;

import com.sigeclin.maestras.model.Cie10;
import com.sigeclin.maestras.repository.Cie10Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cie10")
@RequiredArgsConstructor
public class Cie10RestController {

    private final Cie10Repository cie10Repository;

    @GetMapping("/search")
    public List<Cie10> search(@RequestParam String q) {
        return cie10Repository.search(q, PageRequest.of(0, 10));
    }
}
