package pe.edu.uni.CineStarBarrio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.uni.CineStarBarrio.dto.BoletoDto;
import pe.edu.uni.CineStarBarrio.dto.VentaDto;
import pe.edu.uni.CineStarBarrio.dto.ConfirmacionDto;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CineProcesosService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CineProcesosService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // CLASE PRIVADA (Helper interno)
    private static class DetalleAsientoVenta {
        int asientoId;
        int tipoEntradaId;
        double precio;

        public DetalleAsientoVenta(int asientoId, int tipoEntradaId, double precio) {
            this.asientoId = asientoId;
            this.tipoEntradaId = tipoEntradaId;
            this.precio = precio;
        }
    }

    // === MÉTODO PÚBLICO PRINCIPAL (Venta Directa) ===
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public BoletoDto registrarVenta(VentaDto bean) throws SQLException {
        validarDisponibilidadAsientos(bean.getAsientosIds());

        int usuarioId = getOrCreateUsuario(bean.getClienteNombre(), bean.getClienteApellido(), bean.getClienteTelefono());

        List<DetalleAsientoVenta> detallesVenta = calcularDetallesVenta(bean.getFuncionId(), bean.getAsientosIds());

        double precioTotalReserva = detallesVenta.stream().mapToDouble(d -> d.precio).sum();

        int reservaId = crearReserva(bean.getFuncionId(), usuarioId, bean.getEmpleadoId(), precioTotalReserva);

        asignarAsientosReserva(reservaId, bean.getFuncionId(), detallesVenta);

        generarBoleto(reservaId, precioTotalReserva);

        return obtenerInfoBoleto(reservaId);
    }

    // === MÉTODO PÚBLICO: CONFIRMAR RESERVA (Web -> Pago) ===
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public BoletoDto confirmarReserva(ConfirmacionDto bean) throws SQLException {

        // 1. Validar que la reserva exista y esté Pendiente
        validarReservaPendiente(bean.getReservaId());

        // 2. Actualizar estado a 'Confirmada' Y LOS ASIENTOS A 'Ocupado' (CORRECCIÓN APLICADA)
        actualizarEstadoReservaYAsientos(bean.getReservaId(), bean.getEmpleadoId());

        // 3. Obtener el monto total de la reserva
        Double precioObj = jdbcTemplate.queryForObject(
                "SELECT PrecioTotal FROM RESERVA WHERE ReservaID = ?", Double.class, bean.getReservaId());
        double precioTotal = (precioObj != null) ? precioObj : 0.0;

        // 4. Generar el Boleto
        generarBoleto(bean.getReservaId(), precioTotal);

        // 5. Retornar el boleto generado
        return obtenerInfoBoleto(bean.getReservaId());
    }

    // === MÉTODOS PRIVADOS (Helpers) ===

    private void validarDisponibilidadAsientos(List<Integer> asientosIds) throws SQLException {
        String inParams = asientosIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = String.format("""
            SELECT COUNT(1) FROM ASIENTO
            WHERE AsientoID IN (%s)
            AND Estado <> 'Disponible'""", inParams);

        Integer ocupados = jdbcTemplate.queryForObject(sql, Integer.class, asientosIds.toArray());

        if (ocupados != null && ocupados > 0) {
            throw new SQLException("Uno o más asientos seleccionados ya no están disponibles.");
        }
    }

    private int getOrCreateUsuario(String nombre, String apellido, String telefono) throws SQLException {
        try {
            Integer id = jdbcTemplate.queryForObject("SELECT UsuarioID FROM USUARIO WHERE Telefono = ?", Integer.class, telefono);
            return (id != null) ? id : 0;
        } catch (EmptyResultDataAccessException e) {
            String dummyEmail = telefono + "@cliente.express";
            String sqlInsert = """
                INSERT INTO USUARIO (Nombre, Apellido, Email, PasswordHash, Telefono, Activo, FechaRegistro)
                VALUES (?, ?, ?, 'DUMMY_HASH', ?, 1, SYSDATETIME())""";

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, nombre);
                ps.setString(2, apellido);
                ps.setString(3, dummyEmail);
                ps.setString(4, telefono);
                return ps;
            }, keyHolder);

            Number key = keyHolder.getKey();
            if (key == null) throw new SQLException("Error al crear usuario: No se generó ID.");
            return key.intValue();
        }
    }

    private List<DetalleAsientoVenta> calcularDetallesVenta(int funcionId, List<Integer> asientosIds) {
        List<DetalleAsientoVenta> lista = new ArrayList<>();

        Double precioNormalObj = jdbcTemplate.queryForObject("SELECT PrecioBase FROM FUNCION WHERE FuncionID = ?", Double.class, funcionId);
        Double precioDiscapacitadoObj = jdbcTemplate.queryForObject("SELECT PrecioBase FROM TIPO_ENTRADA WHERE Nombre = 'Persona con discapacidad'", Double.class);
        Double precioAcompananteObj = jdbcTemplate.queryForObject("SELECT PrecioBase FROM TIPO_ENTRADA WHERE Nombre = 'Acompañante'", Double.class);

        double precioNormal = (precioNormalObj != null) ? precioNormalObj : 0.0;
        double precioDiscapacitado = (precioDiscapacitadoObj != null) ? precioDiscapacitadoObj : 0.0;
        double precioAcompanante = (precioAcompananteObj != null) ? precioAcompananteObj : 0.0;

        int ID_ADULTO = 1;
        int ID_DISCAP = 3;
        int ID_ACOMP = 4;

        for (Integer asientoId : asientosIds) {
            String tipoAsiento = jdbcTemplate.queryForObject("SELECT Tipo FROM ASIENTO WHERE AsientoID = ?", String.class, asientoId);

            if ("Discapacitado".equalsIgnoreCase(tipoAsiento)) {
                lista.add(new DetalleAsientoVenta(asientoId, ID_DISCAP, precioDiscapacitado));
            } else if ("Acompañante".equalsIgnoreCase(tipoAsiento)) {
                lista.add(new DetalleAsientoVenta(asientoId, ID_ACOMP, precioAcompanante));
            } else {
                lista.add(new DetalleAsientoVenta(asientoId, ID_ADULTO, precioNormal));
            }
        }
        return lista;
    }

    private int crearReserva(int funcionId, int usuarioId, int empleadoId, double precioTotal) throws SQLException {
        String sql = """
            INSERT INTO RESERVA (CodigoReserva, FuncionID, UsuarioID, Estado, PrecioTotal, RegistradoPor, FechaReserva, FechaExpiracion)
            VALUES (?, ?, ?, 'Confirmada', ?, ?, SYSDATETIME(), DATEADD(MINUTE, 15, SYSDATETIME()))""";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String tempCode = "TEMP-" + System.currentTimeMillis();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, tempCode);
            ps.setInt(2, funcionId);
            ps.setInt(3, usuarioId);
            ps.setDouble(4, precioTotal);
            ps.setInt(5, empleadoId);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new SQLException("Error al crear reserva: ID no generado.");

        int id = key.intValue();
        jdbcTemplate.update("UPDATE RESERVA SET CodigoReserva = ? WHERE ReservaID = ?", "RES-" + String.format("%04d", id), id);
        return id;
    }

    private void asignarAsientosReserva(int reservaId, int funcionId, List<DetalleAsientoVenta> detalles) {
        String sqlInsert = """
            INSERT INTO RESERVA_ASIENTO (ReservaID, FuncionID, AsientoID, TipoEntradaID, PrecioUnitario, Estado, FechaReservaAsiento, FechaExpiracionAsiento)
            VALUES (?, ?, ?, ?, ?, 'Ocupado', SYSDATETIME(), DATEADD(MINUTE, 15, SYSDATETIME()))""";

        String sqlUpdate = "UPDATE ASIENTO SET Estado = 'Ocupado' WHERE AsientoID = ?";

        jdbcTemplate.batchUpdate(sqlInsert, detalles, detalles.size(), (ps, d) -> {
            ps.setInt(1, reservaId);
            ps.setInt(2, funcionId);
            ps.setInt(3, d.asientoId);
            ps.setInt(4, d.tipoEntradaId);
            ps.setDouble(5, d.precio);
        });

        for (DetalleAsientoVenta d : detalles) {
            jdbcTemplate.update(sqlUpdate, d.asientoId);
        }
    }

    private void generarBoleto(int reservaId, double precioTotal) {
        String sql = "INSERT INTO BOLETO (ReservaID, PrecioTotal, MetodoPago, FechaVenta) VALUES (?, ?, 'Efectivo', SYSDATETIME())";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, reservaId);
            ps.setDouble(2, precioTotal);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            int id = key.intValue();
            jdbcTemplate.update("UPDATE BOLETO SET CodigoBoleto = ? WHERE BoletoID = ?", "BOL-" + String.format("%04d", id), id);
        }
    }

    private BoletoDto obtenerInfoBoleto(int reservaId) {
        String sql = """
            SELECT B.CodigoBoleto, R.PrecioTotal, U.Nombre + ' ' + U.Apellido AS Cliente, P.Titulo, S.Nombre AS Sala, F.FechaHoraInicio
            FROM BOLETO B
            JOIN RESERVA R ON B.ReservaID = R.ReservaID
            JOIN USUARIO U ON R.UsuarioID = U.UsuarioID
            JOIN FUNCION F ON R.FuncionID = F.FuncionID
            JOIN PELICULA P ON F.PeliculaID = P.PeliculaID
            JOIN SALA S ON F.SalaID = S.SalaID
            WHERE B.ReservaID = ?""";

        BoletoDto boleto = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                        BoletoDto.builder()
                                .reservaId(reservaId)
                                .codigoBoleto(rs.getString("CodigoBoleto"))
                                .precioTotal(rs.getDouble("PrecioTotal"))
                                .clienteNombreCompleto(rs.getString("Cliente"))
                                .peliculaTitulo(rs.getString("Titulo"))
                                .salaNombre(rs.getString("Sala"))
                                .fechaHoraFuncion(rs.getTimestamp("FechaHoraInicio").toString())
                                .build()
                , reservaId);

        if (boleto != null) {
            String sqlAsientos = """
                SELECT A.Fila + CAST(A.Numero AS VARCHAR) + ' (' + A.Tipo + ')'
                FROM RESERVA_ASIENTO RA JOIN ASIENTO A ON RA.AsientoID = A.AsientoID WHERE RA.ReservaID = ?""";
            boleto.setAsientos(jdbcTemplate.queryForList(sqlAsientos, String.class, reservaId));
        }
        return boleto;
    }

    private void validarReservaPendiente(int reservaId) throws SQLException {
        String sql = "SELECT Estado FROM RESERVA WHERE ReservaID = ?";
        try {
            String estado = jdbcTemplate.queryForObject(sql, String.class, reservaId);

            if (!"Pendiente".equalsIgnoreCase(estado)) {
                throw new SQLException("La reserva no está en estado Pendiente (Estado actual: " + estado + ").");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new SQLException("El código de reserva no existe.");
        }
    }

    // ✅ MÉTODO CORREGIDO: Actualiza Reserva + Reserva_Asiento + Asientos
    private void actualizarEstadoReservaYAsientos(int reservaId, int empleadoId) {
        // 1. Confirmar la Reserva Principal
        String sqlReserva = """
            UPDATE RESERVA
            SET Estado = 'Confirmada',
                RegistradoPor = ?,
                FechaReserva = SYSDATETIME()
            WHERE ReservaID = ?""";
        jdbcTemplate.update(sqlReserva, empleadoId, reservaId);

        // 2. Actualizar los detalles (Reserva_Asiento) a 'Ocupado'
        String sqlDetalle = "UPDATE RESERVA_ASIENTO SET Estado = 'Ocupado' WHERE ReservaID = ?";
        jdbcTemplate.update(sqlDetalle, reservaId);

        // 3. Actualizar los asientos físicos a 'Ocupado' (Para que se pinten blancos)
        String sqlAsiento = """
            UPDATE A
            SET A.Estado = 'Ocupado'
            FROM ASIENTO A
            INNER JOIN RESERVA_ASIENTO RA ON A.AsientoID = RA.AsientoID
            WHERE RA.ReservaID = ?""";
        jdbcTemplate.update(sqlAsiento, reservaId);
    }
}