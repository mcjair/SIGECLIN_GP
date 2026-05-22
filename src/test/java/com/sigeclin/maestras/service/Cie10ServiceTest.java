package com.sigeclin.maestras.service;

import com.sigeclin.maestras.model.Cie10;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Cie10ServiceTest {

    private Cie10Service cie10Service;

    @BeforeEach
    public void setUp() {
        cie10Service = new Cie10Service();
        // Set the path to the real ciex directory in the workspace
        ReflectionTestUtils.setField(cie10Service, "dirPath", "d:/UTP/SISTEMAS/AEAMAN/ciex");
        // Initialize the service (which calls PostConstruct init)
        cie10Service.init();
    }

    @Test
    public void testCacheIsLoaded() {
        int cacheSize = cie10Service.getCacheSize();
        System.out.println("CIE-10 Cache Size: " + cacheSize);
        assertTrue(cacheSize > 0, "Cache should have loaded at least some elements from the CSV files");
    }

    @Test
    public void testSearchByCode() {
        // A00 is Cholera (Colera)
        List<Cie10> results = cie10Service.search("A00");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find some results for A00");
        assertTrue(results.stream().anyMatch(c -> c.getCodigo().startsWith("A00")));
    }

    @Test
    public void testSearchByDescription() {
        // "colera" or "cólera"
        List<Cie10> results = cie10Service.search("colera");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find some results for 'colera'");
    }
}
