package pe.edu.uni.CineStarBarrio.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import pe.edu.uni.CineStarBarrio.dto.PerAutorizadoDto;

import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class PerAutorizadoService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Valida las credenciales del empleado
     */
    public PerAutorizadoDto validarLogin(PerAutorizadoDto request) {
        PerAutorizadoDto response = new PerAutorizadoDto();

        try {
            // VALIDACIÓN 1: Verificar que usuario y clave no estén vacíos
            if (request.getUsuario() == null || request.getUsuario().trim().isEmpty()) {
                response.setEstadoLogin(-1);
                response.setMensaje("Error: El usuario es obligatorio.");
                return response;
            }

            if (request.getClave() == null || request.getClave().trim().isEmpty()) {
                response.setEstadoLogin(-1);
                response.setMensaje("Error: La contraseña es obligatoria.");
                return response;
            }

            // VALIDACIÓN 2: Buscar empleado en la base de datos
            String sql = """
                SELECT 
                    EmpleadoID,
                    Nombre,
                    Apellido,
                    Usuario,
                    ClaveHash,
                    Cargo,
                    FechaContratacion
                FROM dbo.EMPLEADO
                WHERE Usuario = ?
                """;

            PerAutorizadoDto empleado;
            try {
                empleado = jdbcTemplate.queryForObject(sql, new EmpleadoRowMapper(), request.getUsuario());
            } catch (Exception e) {
                response.setEstadoLogin(-1);
                response.setMensaje("Error: Usuario no encontrado.");
                return response;
            }



            // VALIDACIÓN 4: Verificar contraseña
            // IMPORTANTE: En producción deberías usar BCrypt o similar
            if (!request.getClave().equals(empleado.getClave())) {
                response.setEstadoLogin(-1);
                response.setMensaje("Error: Contraseña incorrecta.");
                return response;
            }

            // ✅ LOGIN EXITOSO
            response.setEstadoLogin(1);
            response.setEmpleadoID(empleado.getEmpleadoID());
            response.setNombre(empleado.getNombre());
            response.setApellido(empleado.getApellido());
            response.setNombreCompleto(empleado.getNombre() + " " + empleado.getApellido());
            response.setCargo(empleado.getCargo());
            response.setFechaContratacion(empleado.getFechaContratacion());
            response.setMensaje("✓ Bienvenido(a) " + response.getNombreCompleto() + " - " + response.getCargo());

        } catch (Exception e) {
            response.setEstadoLogin(-1);
            String errorMsg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            response.setMensaje("Error al validar credenciales: " + errorMsg);
        }

        return response;
    }

    /**
     * Obtener información del empleado por ID
     */
    public PerAutorizadoDto obtenerEmpleadoPorID(Integer empleadoID) {
        String sql = """
            SELECT 
                EmpleadoID,
                Nombre,
                Apellido,
                Usuario,
                Cargo,
                FechaContratacion
            FROM dbo.EMPLEADO
            WHERE EmpleadoID = ? AND Activo = 1
            """;

        try {
            return jdbcTemplate.queryForObject(sql, new EmpleadoRowMapper(), empleadoID);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * RowMapper para convertir ResultSet a PerAutorizadoDto
     */
    private static class EmpleadoRowMapper implements RowMapper<PerAutorizadoDto> {
        @Override
        public PerAutorizadoDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            PerAutorizadoDto dto = new PerAutorizadoDto();
            dto.setEmpleadoID(rs.getInt("EmpleadoID"));
            dto.setNombre(rs.getString("Nombre"));
            dto.setApellido(rs.getString("Apellido"));
            dto.setUsuario(rs.getString("Usuario"));

            // Solo mapear ClaveHash si existe en el ResultSet
            try {
                dto.setClave(rs.getString("ClaveHash"));
            } catch (SQLException e) {
                // No hacer nada, el campo no existe
            }

            dto.setCargo(rs.getString("Cargo"));
            dto.setFechaContratacion(rs.getTimestamp("FechaContratacion"));
            dto.setNombreCompleto(dto.getNombre() + " " + dto.getApellido());

            return dto;
        }
    }
}
