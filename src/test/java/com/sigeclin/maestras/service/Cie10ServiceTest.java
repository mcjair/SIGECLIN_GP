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
        // Set the path to the ciex-test directory in the test resources
        ReflectionTestUtils.setField(cie10Service, "dirPath", "src/test/resources/ciex-test");
        // Initialize the service (which calls PostConstruct init)
        cie10Service.init();
    }

    @Test
    public void testCacheIsLoaded() {
        int cacheSize = cie10Service.getCacheSize();
        assertTrue(cacheSize > 0, "Cache should have loaded at least some elements from the CSV files");
    }

    @Test
    public void testSearchByCode() {
        // I10 is Essential Hypertension (included in curated set)
        List<Cie10> results = cie10Service.search("I10");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find I10 - Hipertensión esencial");
        assertTrue(results.stream().anyMatch(c -> c.getCodigo().contains("I10")));
    }

    @Test
    public void testSearchByDescription() {
        // Search for "hipertension" (included in curated set)
        List<Cie10> results = cie10Service.search("hipertension");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find results for 'hipertension'");
    }

    @Test
    public void testSearchFilteredByService() {
        // Search filtered by MEDICINA GENERAL should return hypertension
        List<Cie10> results = cie10Service.search("hipertension", "MEDICINA GENERAL");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find hypertension in MEDICINA GENERAL");
    }

    @Test
    public void testSearchFilteredByServiceNoMatch() {
        // Search filtered by ODONTOLOGIA should NOT return hypertension
        List<Cie10> results = cie10Service.search("hipertension", "ODONTOLOGIA");
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Hypertension should not appear in ODONTOLOGIA");
    }
}
