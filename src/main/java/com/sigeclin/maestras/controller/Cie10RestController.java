package com.sigeclin.maestras.controller;

import com.sigeclin.maestras.model.Cie10;
import com.sigeclin.maestras.service.ICie10Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cie10")
@RequiredArgsConstructor
public class Cie10RestController {

    private final ICie10Service cie10Service;

    @GetMapping("/search")
    public List<Cie10> search(@RequestParam String q, @RequestParam(required = false) String servicio) {
        return cie10Service.search(q, servicio);
    }
}
