package com.sigeclin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.sigeclin.filiacion.model.Usuario;
import com.sigeclin.filiacion.model.Paciente;
import com.sigeclin.filiacion.repository.UsuarioRepository;
import com.sigeclin.filiacion.repository.PacienteRepository;
import com.sigeclin.filiacion.model.TipoDocumento;
import com.sigeclin.filiacion.repository.TipoDocumentoRepository;
import java.time.LocalDate;

@SpringBootApplication
@EnableJpaAuditing
public class SigeclinApplication {

	public static void main(String[] args) {
		SpringApplication.run(SigeclinApplication.class, args);
	}
}
