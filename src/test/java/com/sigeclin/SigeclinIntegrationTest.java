package com.sigeclin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.profiles.active=test",
    "spring.datasource.url=jdbc:h2:mem:testint;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "sigeclin.cie10.dir-path=src/test/resources/ciex-test"
})
class SigeclinIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context, "El contexto de Spring debe cargarse correctamente");
    }

    @Test
    void dataSourceIsConfigured() throws Exception {
        DataSource ds = context.getBean(DataSource.class);
        try (Connection conn = ds.getConnection()) {
            assertTrue(conn.isValid(2), "La conexion a BD debe ser valida");
        }
    }

    @Test
    void allExpectedBeansArePresent() {
        assertNotNull(context.getBean(com.sigeclin.service.IDashboardService.class));
        assertNotNull(context.getBean(com.sigeclin.clinico.service.IConsultaService.class));
        assertNotNull(context.getBean(com.sigeclin.clinico.service.ITriajeService.class));
        assertNotNull(context.getBean(com.sigeclin.clinico.service.IRecetaService.class));
        assertNotNull(context.getBean(com.sigeclin.clinico.service.IHistoriaClinicaService.class));
        assertNotNull(context.getBean(com.sigeclin.clinico.service.IAuditoriaService.class));
        assertNotNull(context.getBean(com.sigeclin.clinico.service.IApoyoDiagnosticoService.class));
        assertNotNull(context.getBean(com.sigeclin.filiacion.service.IPacienteService.class));
        assertNotNull(context.getBean(com.sigeclin.filiacion.service.IPersonalService.class));
        assertNotNull(context.getBean(com.sigeclin.maestras.service.ICie10Service.class));
        assertNotNull(context.getBean(com.sigeclin.maestras.service.IMaestrasService.class));
        assertNotNull(context.getBean(com.sigeclin.seguridad.service.CustomUserDetailsService.class));
    }
}
