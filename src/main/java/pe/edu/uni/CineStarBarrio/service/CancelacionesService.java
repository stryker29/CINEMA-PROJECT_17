package pe.edu.uni.CineStarBarrio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.uni.CineStarBarrio.dto.CancelacionesDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CancelacionesService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String[] CARGOS_AUTORIZADOS = {"Administrador", "Supervisor", "Taquillero"};

    public CancelacionesDto buscarReservaPorCodigo(String codigoReserva) {
        String sql = """
            SELECT
                R.ReservaID,
                R.CodigoReserva,
                R.Estado,
                U.Nombre + ' ' + U.Apellido AS Cliente,
                F.FechaHoraInicio,
                P.Titulo AS Pelicula,
                R.MotivoCancelacion AS Motivo,
                R.FechaCancelacion,
                R.CanceladoPor AS EmpleadoID,
                CASE 
                    WHEN R.CanceladoPor IS NOT NULL THEN
                        (SELECT E.Nombre + ' ' + E.Apellido 
                         FROM dbo.EMPLEADO E 
                         WHERE E.EmpleadoID = R.CanceladoPor)
                    ELSE NULL
                END AS EmpleadoNombre
            FROM dbo.RESERVA R
            JOIN dbo.USUARIO U ON R.UsuarioID = U.UsuarioID
            JOIN dbo.FUNCION F ON R.FuncionID = F.FuncionID
            JOIN dbo.PELICULA P ON F.PeliculaID = P.PeliculaID
            WHERE R.CodigoReserva = ?
            """;
        try {
            return jdbcTemplate.queryForObject(sql, new ReservaDetalladaRowMapper(), codigoReserva);
        } catch (Exception e) {
            return null;
        }
    }

    public List<CancelacionesDto> buscarReservasPorCliente(String nombreCliente) {
        String sql = """
            SELECT 
                R.ReservaID, 
                R.CodigoReserva, 
                R.Estado, 
                U.Nombre + ' ' + U.Apellido AS Cliente, 
                F.FechaHoraInicio, 
                P.Titulo AS Pelicula, 
                R.MotivoCancelacion AS Motivo,
                R.FechaCancelacion,
                R.CanceladoPor AS EmpleadoID,
                CASE 
                    WHEN R.CanceladoPor IS NOT NULL THEN
                        (SELECT E.Nombre + ' ' + E.Apellido 
                         FROM dbo.EMPLEADO E 
                         WHERE E.EmpleadoID = R.CanceladoPor)
                    ELSE NULL
                END AS EmpleadoNombre
            FROM dbo.RESERVA R
            JOIN dbo.USUARIO U ON R.UsuarioID = U.UsuarioID
            JOIN dbo.FUNCION F ON R.FuncionID = F.FuncionID
            JOIN dbo.PELICULA P ON F.PeliculaID = P.PeliculaID
            WHERE U.Nombre LIKE ? OR U.Apellido LIKE ?
            ORDER BY R.FechaReserva DESC
            """;

        String parametroBusqueda = "%" + nombreCliente + "%";
        return jdbcTemplate.query(sql, new ReservaDetalladaRowMapper(), parametroBusqueda, parametroBusqueda);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CancelacionesDto cancelarReserva(CancelacionesDto request) {
        CancelacionesDto response = new CancelacionesDto();

        try {
            String sqlReserva = """
                SELECT R.ReservaID, R.CodigoReserva, R.Estado
                FROM dbo.RESERVA R
                WHERE R.ReservaID = ?
                """;
            Map<String, Object> reserva = jdbcTemplate.queryForMap(sqlReserva, request.getReservaID());
            String estadoActual = (String) reserva.get("Estado");

            if ("Cancelada".equals(estadoActual)) {
                response.setEstadoCancelar(-1);
                response.setMensaje("Error: La reserva ya está cancelada.");
                return response;
            }

            if (!estadoActual.equals("Pendiente") && !estadoActual.equals("Confirmada")) {
                response.setEstadoCancelar(-1);
                response.setMensaje("Error: La reserva en estado '" + estadoActual + "' no puede ser cancelada.");
                return response;
            }

            String sqlEmpleado = """
                SELECT EmpleadoID, Nombre, Apellido, Cargo
                FROM dbo.EMPLEADO
                WHERE EmpleadoID = ?
                """;
            Map<String, Object> empleado = jdbcTemplate.queryForMap(sqlEmpleado, request.getEmpleadoID());

            String cargo = (String) empleado.get("Cargo");

            boolean cargoValido = false;
            for (String cargoPermitido : CARGOS_AUTORIZADOS) {
                if (cargoPermitido.equals(cargo)) {
                    cargoValido = true;
                    break;
                }
            }

            if (!cargoValido) {
                response.setEstadoCancelar(-1);
                response.setMensaje("Error: El empleado con cargo '" + cargo + "' no tiene permisos para cancelar.");
                return response;
            }

            if (request.getMotivo() == null || request.getMotivo().trim().length() < 10 ||
                    request.getMotivo().trim().length() > 200) {
                response.setEstadoCancelar(-1);
                response.setMensaje("Error: El motivo debe tener entre 10 y 200 caracteres.");
                return response;
            }

            //  Actualizar la reserva con los datos de cancelación
            String sqlCancelar = """
                UPDATE dbo.RESERVA 
                SET Estado = 'Cancelada',
                    FechaCancelacion = SYSDATETIME(),
                    CanceladoPor = ?,
                    MotivoCancelacion = ?
                WHERE ReservaID = ?
                """;
            jdbcTemplate.update(sqlCancelar, request.getEmpleadoID(), request.getMotivo(), request.getReservaID());

            //  Liberar los asientos
            String sqlLiberarAsientos = """
                UPDATE A
                SET A.Estado = 'Disponible'
                FROM dbo.ASIENTO A
                INNER JOIN dbo.RESERVA_ASIENTO RA ON A.AsientoID = RA.AsientoID
                WHERE RA.ReservaID = ?
                """;
            jdbcTemplate.update(sqlLiberarAsientos, request.getReservaID());

            // Actualizar RESERVA_ASIENTO
            String sqlActualizarRA = """
                UPDATE dbo.RESERVA_ASIENTO
                SET Estado = 'Cancelado'
                WHERE ReservaID = ?
                """;
            jdbcTemplate.update(sqlActualizarRA, request.getReservaID());

            String nombreEmpleado = empleado.get("Nombre") + " " + empleado.get("Apellido");
            String codigoReserva = (String) reserva.get("CodigoReserva");

            response.setEstadoCancelar(1);
            response.setMensaje("✅ Reserva " + codigoReserva + " cancelada exitosamente por " +
                    nombreEmpleado + ". Los asientos han sido liberados.");

        } catch (Exception e) {
            response.setEstadoCancelar(-1);
            String errorMsg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
            response.setMensaje("Error al cancelar reserva: " + errorMsg);
        }

        return response;
    }


    public List<Map<String, Object>> obtenerHistorialCancelaciones() {
        String sql = """
            SELECT 
                R.ReservaID,
                R.CodigoReserva,
                R.Estado,
                U.Nombre + ' ' + U.Apellido AS Cliente,
                P.Titulo AS Pelicula,
                F.FechaHoraInicio,
                R.FechaCancelacion,
                E.Nombre + ' ' + E.Apellido AS CanceladoPor,
                E.Cargo AS Rol,
                R.MotivoCancelacion AS Motivo
            FROM dbo.RESERVA R
            INNER JOIN dbo.USUARIO U ON R.UsuarioID = U.UsuarioID
            INNER JOIN dbo.FUNCION F ON R.FuncionID = F.FuncionID
            INNER JOIN dbo.PELICULA P ON F.PeliculaID = P.PeliculaID
            LEFT JOIN dbo.EMPLEADO E ON R.CanceladoPor = E.EmpleadoID
            WHERE R.Estado = 'Cancelada'
            ORDER BY R.FechaCancelacion DESC
            """;
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }



    public Map<String, Object> compararLiberacionAsientos(int reservaId) {
        try {
            String sqlReserva = """
                SELECT 
                    R.CodigoReserva, 
                    R.Estado, 
                    R.FechaCancelacion
                FROM dbo.RESERVA R 
                WHERE R.ReservaID = ?
                """;
            Map<String, Object> reserva = jdbcTemplate.queryForMap(sqlReserva, reservaId);

            String sqlAsientos = """
                SELECT 
                    A.AsientoID, 
                    A.Fila, 
                    A.Numero, 
                    A.Estado AS EstadoActual, 
                    RA.Estado AS EstadoEnReserva
                FROM dbo.ASIENTO A
                INNER JOIN dbo.RESERVA_ASIENTO RA ON A.AsientoID = RA.AsientoID
                WHERE RA.ReservaID = ?
                """;
            List<Map<String, Object>> asientos = jdbcTemplate.queryForList(sqlAsientos, reservaId);

            int liberados = 0;
            for (Map<String, Object> asiento : asientos) {
                String estadoActual = (String) asiento.get("EstadoActual");
                if ("Disponible".equals(estadoActual)) {
                    liberados++;
                }
            }
            int noLiberados = asientos.size() - liberados;

            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("total", asientos.size());
            estadisticas.put("liberados", liberados);
            estadisticas.put("noLiberados", noLiberados);
            estadisticas.put("todoLiberado", noLiberados == 0);

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("reserva", reserva);
            resultado.put("asientos", asientos);
            resultado.put("estadisticas", estadisticas);
            return resultado;

        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyMap();
        }
    }
    //  Obtener reservas activas (Confirmadas y Pendientes)
    public List<Map<String, Object>> obtenerReservasActivas() {
        String sql = """
        SELECT 
            R.ReservaID,
            R.CodigoReserva,
            R.Estado,
            U.Nombre + ' ' + U.Apellido AS Cliente,
            P.Titulo AS Pelicula,
            F.FechaHoraInicio,
            R.FechaReserva,
            R.PrecioTotal,
            S.Nombre AS Sala,
            E.Nombre + ' ' + E.Apellido AS RegistradoPor,
            (SELECT COUNT(*) 
             FROM dbo.RESERVA_ASIENTO RA 
             WHERE RA.ReservaID = R.ReservaID) AS CantidadAsientos
        FROM dbo.RESERVA R
        INNER JOIN dbo.USUARIO U ON R.UsuarioID = U.UsuarioID
        INNER JOIN dbo.FUNCION F ON R.FuncionID = F.FuncionID
        INNER JOIN dbo.PELICULA P ON F.PeliculaID = P.PeliculaID
        INNER JOIN dbo.SALA S ON F.SalaID = S.SalaID
        LEFT JOIN dbo.EMPLEADO E ON R.RegistradoPor = E.EmpleadoID
        WHERE R.Estado IN ('Confirmada', 'Pendiente')
        ORDER BY R.FechaReserva DESC
        """;
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    private static class ReservaDetalladaRowMapper implements RowMapper<CancelacionesDto> {
        @Override
        public CancelacionesDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            CancelacionesDto dto = new CancelacionesDto();

            dto.setReservaID(rs.getInt("ReservaID"));
            dto.setCodigoReserva(rs.getString("CodigoReserva"));
            dto.setEstado(rs.getString("Estado"));
            dto.setCliente(rs.getString("Cliente"));
            dto.setFechaHoraInicio(rs.getTimestamp("FechaHoraInicio"));
            dto.setPelicula(rs.getString("Pelicula"));

            String motivo = rs.getString("Motivo");
            dto.setMotivo(motivo != null ? motivo : "");

            java.sql.Timestamp fechaCancelacion = rs.getTimestamp("FechaCancelacion");
            dto.setFechaCancelacion(fechaCancelacion);

            int empleadoID = rs.getInt("EmpleadoID");
            dto.setEmpleadoID(rs.wasNull() ? null : empleadoID);

            String empleadoNombre = rs.getString("EmpleadoNombre");
            dto.setEmpleadoNombre(empleadoNombre);

            return dto;
        }
    }
}