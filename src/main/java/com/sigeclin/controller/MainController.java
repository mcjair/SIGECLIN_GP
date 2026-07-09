package com.sigeclin.controller;

import com.sigeclin.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private static final String ROL_MEDICO_GENERAL = "MEDICO_GENERAL";

    private final IDashboardService dashboardService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(org.springframework.ui.Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            String roleStr = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst().orElse("ADMIN");
            
            if (!"ADMIN".equals(roleStr) && !"SUPERADMIN".equals(roleStr)) {
                return switch (roleStr) {
                    case ROL_MEDICO_GENERAL -> "redirect:/consulta/modulo/medicina_general";
                    case "ENFERMERIA", "TRIAJE" -> "redirect:/triaje/nuevo";
                    case "ADMISION" -> "redirect:/admission/registro";
                    case "CAJA" -> "redirect:/caja/pago";
                    case "ODONTOLOGIA" -> "redirect:/consulta/modulo/odontologia";
                    case "PSICOLOGIA" -> "redirect:/consulta/modulo/psicologia";
                    case "NUTRICION" -> "redirect:/consulta/modulo/nutricion";
                    case "OBSTETRICIA" -> "redirect:/consulta/modulo/obstetricia";
                    case "FARMACIA" -> "redirect:/apoyo/farmacia";
                    case "LABORATORIO" -> "redirect:/apoyo/laboratorio";
                    default -> "dashboard";
                };
            }
        }
        dashboardService.cargarDatosDashboard(model);
        return "dashboard";
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public Map<String, Object> getDashboardStats(@RequestParam(required = false, defaultValue = "day") String filter) {
        return dashboardService.getDashboardStats(filter);
    }

    private void procesarSincronizacionUsuario(Map<String, Object> p, String passHash) {
        Integer idPersona = (Integer) p.get("id_persona");
        String nombres = (String) p.get("nombres");
        String apellidoPaterno = (String) p.get("apellido_paterno");

        // Generar username: 1era letra nombre + apellido paterno
        String baseUser = nombres.substring(0, 1).toLowerCase() + apellidoPaterno.toLowerCase();
        baseUser = baseUser.replaceAll("[^a-z0-9]", "");

        // Verificar colisiones
        String username = baseUser;
        int suffix = 1;
        while (true) {
            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM filiacion.usuario WHERE username = ?",
                Integer.class,
                username
            );
            if (exists != null && exists == 0) break;
            username = baseUser + suffix;
            suffix++;
        }

        // Crear usuario
        jdbcTemplate.update(
            "INSERT INTO filiacion.usuario (id_usuario, username, password_hash, cuenta_bloqueada, intentos_fallidos, requiere_cambio_password, fecha_creacion) " +
            "VALUES (?, ?, ?, false, 0, false, CURRENT_TIMESTAMP)",
            idPersona, username, passHash
        );

        // Asignar Rol por defecto según tipo de personal
        Integer idTipoPersonal = jdbcTemplate.queryForObject(
            "SELECT id_tipo_personal FROM filiacion.personal WHERE id_personal = ?",
            Integer.class,
            idPersona
        );

        String rolCode = ROL_MEDICO_GENERAL;
        if (idTipoPersonal != null) {
            rolCode = switch (idTipoPersonal) {
                case 1, 2 -> ROL_MEDICO_GENERAL;
                case 3 -> "ODONTOLOGIA";
                case 4 -> "ENFERMERIA";
                case 5 -> "PSICOLOGIA";
                case 6 -> "NUTRICION";
                case 7 -> "OBSTETRICIA";
                default -> ROL_MEDICO_GENERAL;
            };
        }

        Integer idRol = jdbcTemplate.queryForObject(
            "SELECT id_rol FROM seguridad.rol WHERE codigo = ?",
            Integer.class,
            rolCode
        );

        if (idRol != null) {
            jdbcTemplate.update(
                "INSERT INTO seguridad.usuario_rol (id_usuario, id_rol) VALUES (?, ?) ON CONFLICT DO NOTHING",
                idPersona, idRol
            );
        }

        // Enlazar en personal
        jdbcTemplate.update(
            "UPDATE filiacion.personal SET id_usuario = ? WHERE id_personal = ?",
            idPersona, idPersona
        );
    }

    @GetMapping("/dev/sync-usuarios")
    @ResponseBody
    public String syncUsuariosRetroactivo() {
        try {
            // Obtener personal sin usuario
            java.util.List<Map<String, Object>> personalSinUsuario = jdbcTemplate.queryForList(
                "SELECT p.id_persona, p.nombres, p.apellido_paterno " +
                "FROM filiacion.personal per " +
                "JOIN filiacion.persona p ON per.id_personal = p.id_persona " +
                "LEFT JOIN filiacion.usuario u ON p.id_persona = u.id_usuario " +
                "WHERE u.id_usuario IS NULL"
            );

            int count = 0;
            String defaultPass = new String(java.util.Base64.getDecoder().decode("YWRtaW4="), java.nio.charset.StandardCharsets.UTF_8);
            String passHash = passwordEncoder.encode(defaultPass);

            for (Map<String, Object> p : personalSinUsuario) {
                procesarSincronizacionUsuario(p, passHash);
                count++;
            }
            return "Sincronización completada. Usuarios creados retroactivamente: " + count;
        } catch (Exception e) {
            log.error("Error en sincronización manual: ", e);
            return "Error al colocar: " + e.getMessage();
        }
    }

    @GetMapping("/dev/list-usuarios")
    @ResponseBody
    public String listUsuarios() {
        StringBuilder sb = new StringBuilder();
        try {
            java.util.List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "SELECT u.id_usuario, u.username, p.nombres, p.apellido_paterno " +
                "FROM filiacion.usuario u " +
                "JOIN filiacion.persona p ON u.id_usuario = p.id_persona"
            );
            sb.append("Lista de usuarios activos:\n").append(list.toString());
        } catch (Exception e) {
            sb.append("Error al listar: ").append(e.getMessage());
        }
        return sb.toString();
    }
}
