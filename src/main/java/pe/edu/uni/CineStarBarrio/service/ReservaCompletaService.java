package pe.edu.uni.CineStarBarrio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.uni.CineStarBarrio.dto.ClienteDto;
import pe.edu.uni.CineStarBarrio.dto.ReservaCompletaDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReservaCompletaService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ==================== GESTIÓN DE CLIENTES ====================

    /**
     * Registrar nuevo cliente (SIN contraseña requerida)
     */
    @Transactional
    public ClienteDto registrarCliente(ClienteDto bean) {
        try {
            // Validaciones básicas
            if (bean.getNombre() == null || bean.getNombre().trim().length() < 2) {
                throw new RuntimeException("❌ El nombre debe tener al menos 2 caracteres");
            }

            if (bean.getApellido() == null || bean.getApellido().trim().length() < 2) {
                throw new RuntimeException("❌ El apellido debe tener al menos 2 caracteres");
            }

            if (bean.getEmail() == null || !bean.getEmail().contains("@")) {
                throw new RuntimeException("❌ Email inválido");
            }

            // Verificar si el email ya existe
            Integer existe = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM USUARIO WHERE Email = ?",
                    Integer.class, bean.getEmail()
            );

            if (existe != null && existe > 0) {
                throw new RuntimeException("❌ El email ya está registrado");
            }

            // Insertar usuario (sin contraseña)
            jdbcTemplate.update("""
                INSERT INTO USUARIO (Nombre, Apellido, Email, Telefono, PasswordHash, Activo)
                VALUES (?, ?, ?, ?, 1234567, 1)
            """,
                    bean.getNombre().trim(),
                    bean.getApellido().trim(),
                    bean.getEmail().trim(),
                    bean.getTelefono() != null ? bean.getTelefono().trim() : null
            );

            // Obtener ID generado
            Integer usuarioID = jdbcTemplate.queryForObject(
                    "SELECT UsuarioID FROM USUARIO WHERE Email = ?",
                    Integer.class, bean.getEmail()
            );

            bean.setUsuarioID(usuarioID);
            bean.setSuccess(true);
            bean.setMensaje("✅ Cliente registrado exitosamente");

            return bean;

        } catch (Exception e) {
            bean.setSuccess(false);
            bean.setMensaje("❌ Error: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Buscar cliente por email
     */
    public ClienteDto buscarClientePorEmail(String email) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT UsuarioID, Nombre, Apellido, Email, Telefono, Activo
                FROM USUARIO
                WHERE Email = ? AND Activo = 1
            """, email);

            return ClienteDto.builder()
                    .usuarioID((Integer) row.get("UsuarioID"))
                    .nombre((String) row.get("Nombre"))
                    .apellido((String) row.get("Apellido"))
                    .email((String) row.get("Email"))
                    .telefono((String) row.get("Telefono"))
                    .activo((Boolean) row.get("Activo"))
                    .success(true)
                    .mensaje("✅ Cliente encontrado")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("❌ Cliente no encontrado");
        }
    }

    /**
     * Buscar clientes por nombre (búsqueda parcial)
     */
    public List<ClienteDto> buscarClientesPorNombre(String nombre) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT UsuarioID, Nombre, Apellido, Email, Telefono, Activo
                FROM USUARIO
                WHERE (Nombre LIKE ? OR Apellido LIKE ?) AND Activo = 1
            """, "%" + nombre + "%", "%" + nombre + "%");

            List<ClienteDto> clientes = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                clientes.add(ClienteDto.builder()
                        .usuarioID((Integer) row.get("UsuarioID"))
                        .nombre((String) row.get("Nombre"))
                        .apellido((String) row.get("Apellido"))
                        .email((String) row.get("Email"))
                        .telefono((String) row.get("Telefono"))
                        .activo((Boolean) row.get("Activo"))
                        .build());
            }

            if (clientes.isEmpty()) {
                throw new RuntimeException("❌ No se encontraron clientes");
            }

            return clientes;

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    // ==================== CREAR RESERVA COMPLETA ====================

    @Transactional
    public ReservaCompletaDto crearReserva(ReservaCompletaDto bean) {

        try {
            // ========== 1. VALIDACIONES INICIALES ==========
            validarCliente(bean.getIdCliente());
            validarFuncion(bean.getIdFuncion());
            validarAsientos(bean.getAsientosSeleccionados());

            // Establecer empleado por defecto si no viene
            if (bean.getIdEmpleado() == null) {
                bean.setIdEmpleado(1);
            }

            bean.setCantidadEntradas(bean.getAsientosSeleccionados().size());

            // ========== 2. BLOQUEAR FUNCIÓN (Evitar condiciones de carrera) ==========
            jdbcTemplate.update("""
                UPDATE FUNCION WITH (UPDLOCK, HOLDLOCK)
                SET AsientosDisponibles = AsientosDisponibles
                WHERE FuncionID = ?
            """, bean.getIdFuncion());

            // ========== 3. VALIDAR CAPACIDAD ==========
            Integer capacidadActual = jdbcTemplate.queryForObject(
                    "SELECT AsientosDisponibles FROM FUNCION WHERE FuncionID = ?",
                    Integer.class, bean.getIdFuncion()
            );

            bean.setCapacidadInicial(capacidadActual);

            if (capacidadActual == null || capacidadActual < bean.getCantidadEntradas()) {
                throw new RuntimeException(
                        String.format("❌ No hay suficientes asientos. Disponibles: %d, Solicitados: %d",
                                capacidadActual, bean.getCantidadEntradas())
                );
            }

            // ========== 4. RESOLVER Y VALIDAR ASIENTOS ==========
            resolverAsientos(bean);

            // ========== 5. GENERAR CÓDIGO DE RESERVA ==========
            String codigoReserva = jdbcTemplate.queryForObject("""
                SELECT 'RES-' + RIGHT('00' + CAST(ISNULL(MAX(ReservaID), 0) + 1 AS VARCHAR(5)), 5)
                FROM RESERVA
            """, String.class);

            bean.setCodigoReserva(codigoReserva);

            // ========== 6. CALCULAR PRECIOS ==========
            double precioTotal = calcularPrecioTotal(bean);
            bean.setPrecioTotal(precioTotal);

            // ========== 7. CREAR RESERVA PRINCIPAL ==========
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime expiracion = ahora.plusMinutes(15);

            // ✅ CAMBIO 1: 'Reservado' → 'Pendiente'
            jdbcTemplate.update("""
                INSERT INTO RESERVA (
                    CodigoReserva, FuncionID, UsuarioID,
                    FechaReserva, FechaExpiracion,
                    Estado, PrecioTotal, RegistradoPor
                ) VALUES (?, ?, ?, ?, ?, 'Pendiente', ?, ?)
            """,
                    codigoReserva,
                    bean.getIdFuncion(),
                    bean.getIdCliente(),
                    ahora,
                    expiracion,
                    precioTotal,
                    bean.getIdEmpleado()
            );

            // Obtener ID de la reserva
            Integer reservaId = jdbcTemplate.queryForObject(
                    "SELECT ReservaID FROM RESERVA WHERE CodigoReserva = ?",
                    Integer.class, codigoReserva
            );

            // ========== 8. RESERVAR CADA ASIENTO ==========
            for (var asiento : bean.getAsientosSeleccionados()) {
                // ✅ CAMBIO 2: 'Reservado' → 'Pendiente'
                jdbcTemplate.update("""
                    INSERT INTO RESERVA_ASIENTO (
                        ReservaID, FuncionID, AsientoID,
                        TipoEntradaID, PrecioUnitario,
                        FechaReservaAsiento, FechaExpiracionAsiento,
                        Estado
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'Pendiente')
                """,
                        reservaId,
                        bean.getIdFuncion(),
                        asiento.getAsientoId(),
                        asiento.getTipoEntradaId(),
                        asiento.getPrecio(),
                        ahora,
                        expiracion
                );

                // Actualizar estado del asiento a "Reservado"
                jdbcTemplate.update("""
                    UPDATE ASIENTO
                    SET Estado = 'Reservado'
                    WHERE AsientoID = ?
                """, asiento.getAsientoId());
            }

            // ========== 9. ACTUALIZAR DISPONIBILIDAD ==========
            jdbcTemplate.update("""
                UPDATE FUNCION
                SET AsientosDisponibles = AsientosDisponibles - ?
                WHERE FuncionID = ?
            """, bean.getCantidadEntradas(), bean.getIdFuncion());

            bean.setCapacidadFinal(capacidadActual - bean.getCantidadEntradas());

            // ========== 10. CONFIGURAR RESPUESTA ==========
            bean.setFechaReserva(ahora.format(FORMATTER));
            bean.setFechaExpiracion(expiracion.format(FORMATTER));
            // ✅ CAMBIO 3: 'Reservado' → 'Pendiente'
            bean.setEstado("Pendiente");
            bean.setSuccess(true);

            List<String> ubicaciones = new ArrayList<>();
            for (var a : bean.getAsientosSeleccionados()) {
                ubicaciones.add(a.getUbicacion());
            }

            bean.setMensaje(String.format(
                    "✅ Reserva exitosa | Código: %s | Asientos: %s | Total: S/ %.2f | Expira: %s",
                    codigoReserva,
                    String.join(", ", ubicaciones),
                    precioTotal,
                    expiracion.format(FORMATTER)
            ));

            return bean;

        } catch (Exception e) {
            bean.setSuccess(false);
            bean.setMensaje("❌ Error: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // ==================== VALIDACIONES ====================

    private void validarCliente(Integer idCliente) {
        if (idCliente == null) {
            throw new RuntimeException("❌ Debe proporcionar un ID de cliente válido.");
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM USUARIO WHERE UsuarioID = ? AND Activo = 1",
                Integer.class, idCliente
        );

        if (count == null || count == 0) {
            throw new RuntimeException("❌ El cliente con ID " + idCliente + " no existe o no está activo.");
        }
    }

    private void validarFuncion(Integer idFuncion) {
        if (idFuncion == null) {
            throw new RuntimeException("❌ Debe seleccionar una función válida.");
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM FUNCION WHERE FuncionID = ? AND Estado = 'Programada'",
                Integer.class, idFuncion
        );

        if (count == null || count == 0) {
            throw new RuntimeException("❌ La función no existe o no está disponible para reservas.");
        }
    }

    private void validarAsientos(List<ReservaCompletaDto.AsientoSeleccionado> asientos) {
        if (asientos == null || asientos.isEmpty()) {
            throw new RuntimeException("❌ Debe seleccionar al menos un asiento.");
        }

        if (asientos.size() > 3) {
            throw new RuntimeException("❌ Máximo 3 asientos por reserva.");
        }

        for (var asiento : asientos) {
            if (asiento.getFila() == null || asiento.getNumero() == null) {
                throw new RuntimeException("❌ Asiento inválido: fila y número son requeridos.");
            }

            if (asiento.getTipoEntradaId() == null) {
                throw new RuntimeException("❌ Debe especificar el tipo de entrada para el asiento " + asiento.getUbicacion());
            }
        }
    }

    // ==================== RESOLVER ASIENTOS ====================

    private void resolverAsientos(ReservaCompletaDto bean) {
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new ArrayList<>();
        params.add(bean.getIdFuncion());

        // Construir cláusula WHERE dinámica
        for (int i = 0; i < bean.getAsientosSeleccionados().size(); i++) {
            if (i > 0) whereClause.append(" OR ");
            whereClause.append("(A.Fila = ? AND A.Numero = ?)");

            var asiento = bean.getAsientosSeleccionados().get(i);
            params.add(asiento.getFila());
            params.add(asiento.getNumero());
        }

        String sql = String.format("""
            SELECT 
                A.AsientoID,
                A.Fila,
                A.Numero,
                A.Estado,
                A.Tipo,
                A.AsientoRelacionadoID
            FROM ASIENTO A
            INNER JOIN FUNCION F ON A.SalaID = F.SalaID
            WHERE F.FuncionID = ?
              AND A.Activo = 1
              AND (%s)
        """, whereClause);

        List<Map<String, Object>> asientosBD = jdbcTemplate.queryForList(sql, params.toArray());

        if (asientosBD.size() != bean.getAsientosSeleccionados().size()) {
            throw new RuntimeException("❌ Algunos asientos no existen en la sala de esta función.");
        }

        // Validar estado y asociar datos
        List<String> noDisponibles = new ArrayList<>();

        for (int i = 0; i < asientosBD.size(); i++) {
            var asientoBD = asientosBD.get(i);
            var asientoDTO = bean.getAsientosSeleccionados().get(i);

            String estado = (String) asientoBD.get("Estado");
            String ubicacion = asientoBD.get("Fila") + "" + asientoBD.get("Numero");

            if (!"Disponible".equalsIgnoreCase(estado)) {
                noDisponibles.add(ubicacion + " (" + estado + ")");
            }

            // Asociar datos del asiento
            asientoDTO.setAsientoId((Integer) asientoBD.get("AsientoID"));
            asientoDTO.setTipoAsiento((String) asientoBD.get("Tipo"));
            asientoDTO.setEstadoAsiento(estado);

            // Validar reglas de negocio
            validarReglasNegocio(asientoDTO);
        }

        if (!noDisponibles.isEmpty()) {
            throw new RuntimeException(
                    "❌ Los siguientes asientos NO están disponibles: " + String.join(", ", noDisponibles)
            );
        }
    }

    private void validarReglasNegocio(ReservaCompletaDto.AsientoSeleccionado asiento) {
        // Obtener nombre del tipo de entrada
        String nombreTipo = jdbcTemplate.queryForObject(
                "SELECT Nombre FROM TIPO_ENTRADA WHERE TipoEntradaID = ?",
                String.class, asiento.getTipoEntradaId()
        );

        asiento.setNombreTipoEntrada(nombreTipo);

        // REGLA 1: Asientos para discapacitados
        if ("Discapacitado".equalsIgnoreCase(asiento.getTipoAsiento())) {
            if (!"Persona con discapacidad".equalsIgnoreCase(nombreTipo)) {
                throw new RuntimeException(
                        "❌ El asiento " + asiento.getUbicacion() +
                                " es para personas con discapacidad. Debe usar el tipo de entrada correspondiente."
                );
            }
        }

        // REGLA 2: Asientos de acompañante
        if ("Acompañante".equalsIgnoreCase(asiento.getTipoAsiento())) {
            if (!"Acompañante".equalsIgnoreCase(nombreTipo)) {
                throw new RuntimeException(
                        "❌ El asiento " + asiento.getUbicacion() +
                                " es de acompañante. Debe usar el tipo de entrada correspondiente."
                );
            }
        }
    }

    private double calcularPrecioTotal(ReservaCompletaDto bean) {
        double total = 0.0;

        for (var asiento : bean.getAsientosSeleccionados()) {
            Double precio = jdbcTemplate.queryForObject(
                    "SELECT PrecioBase FROM TIPO_ENTRADA WHERE TipoEntradaID = ?",
                    Double.class, asiento.getTipoEntradaId()
            );

            if (precio == null) {
                throw new RuntimeException("❌ No se pudo obtener el precio para el tipo de entrada " + asiento.getTipoEntradaId());
            }

            asiento.setPrecio(precio);
            total += precio;
        }

        return total;
    }

    // ==================== CARTELERA Y ASIENTOS ====================

    public List<Map<String, Object>> obtenerCartelera() {
        String sql = """
        SELECT 
            F.FuncionID,
            F.SalaID,
            P.Titulo AS Pelicula,
            S.Nombre AS Sala,
            S.TipoSala AS TipoSala,
            F.FechaHoraInicio,
            F.PrecioBase,
            F.Estado,
            F.AsientosDisponibles
        FROM FUNCION F
        INNER JOIN PELICULA P ON F.PeliculaID = P.PeliculaID
        INNER JOIN SALA S ON F.SalaID = S.SalaID
        WHERE F.Estado = 'Programada'
          AND F.FechaHoraInicio > GETDATE()
          AND F.AsientosDisponibles > 0
        ORDER BY F.FechaHoraInicio
    """;

        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> listarAsientosPorFuncion(Integer funcionId) {
        String sql = """
            SELECT 
                A.AsientoID AS asientoId,
                A.Fila AS fila,
                A.Numero AS numero,
                A.Estado AS estado,
                A.Tipo AS tipo,
                A.SalaID AS salaId
            FROM ASIENTO A
            INNER JOIN FUNCION F ON F.SalaID = A.SalaID
            WHERE F.FuncionID = ?
              AND A.Activo = 1
            ORDER BY A.Fila, A.Numero
        """;

        return jdbcTemplate.queryForList(sql, funcionId);
    }
}