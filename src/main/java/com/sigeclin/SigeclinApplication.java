package com.sigeclin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del Sistema Integrado de Gestión Clínica (SIGECLIN).
 * <p>
 * Aplica principios SOLID:
 * <ul>
 *   <li><b>SRP</b>: Servicios con responsabilidades únicas (ej. RecetaService separado de ConsultaService)</li>
 *   <li><b>DIP/OCP</b>: Controladores dependen de interfaces de servicio ({@code I*Service})</li>
 *   <li><b>Validación</b>: {@code @Valid} + {@code BindingResult} en endpoints POST</li>
 * </ul>
 */
@SpringBootApplication
public class SigeclinApplication {

	public static void main(String[] args) {
		SpringApplication.run(SigeclinApplication.class, args);
	}
}
