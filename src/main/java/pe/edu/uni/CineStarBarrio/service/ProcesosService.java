package pe.edu.uni.CineStarBarrio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.uni.CineStarBarrio.dto.FuncionDto;
import pe.edu.uni.CineStarBarrio.dto.PeliculaDto;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
public class ProcesosService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // RF1 - PARTE 1: Registrar nueva película
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public PeliculaDto registrarPelicula(PeliculaDto bean) throws SQLException {
        try {
            // Validaciones exhaustivas
            validarTituloPelicula(bean.getTitulo());
            validarGeneroPelicula(bean.getGenero());
            validarDuracionPelicula(bean.getDuracion());
            validarClasificacionPelicula(bean.getClasificacion());
            validarDirectorPelicula(bean.getDirector());
            validarSinopsisPelicula(bean.getSinopsis());
            validarFechaEstreno(bean.getFechaEstreno());
            validarEmpleado(bean.getCreadoPor());

            // Registrar película
            int peliculaId = insertarPelicula(bean);

            // Configurar variables de salida
            bean.setPeliculaId(peliculaId);
            bean.setActiva(true);
            bean.setEstado(1);
            bean.setMensaje("Película registrada exitosamente");

            return bean;

        } catch (SQLException e) {
            bean.setEstado(0);
            bean.setMensaje("ERROR: " + e.getMessage());
            throw e;
        }
    }

    // RF1 - PARTE 2: Registrar función para película existente
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public FuncionDto registrarFuncion(FuncionDto bean) throws SQLException {
        try {
            // Validaciones exhaustivas
            validarPelicula(bean.getPeliculaId());
            validarSala(bean.getSalaId());
            validarEmpleado(bean.getCreadoPor());
            validarPrecioBase(bean.getPrecioBase());

            // Convertir String a DateTime
            LocalDateTime fechaHora = convertirFechaHora(bean.getFechaHoraInicio());

            // Validaciones de fecha/hora
            validarFechaHoraFutura(fechaHora);
            validarAnticipacionMinima(fechaHora);
            validarHorarioSala(bean.getSalaId(), fechaHora);

            // Obtener capacidad de la sala
            int capacidadSala = obtenerCapacidadSala(bean.getSalaId());
            bean.setAsientosDisponibles(capacidadSala);
            bean.setEstado("Programada");

            // Registrar la función
            int funcionId = insertarFuncion(bean, fechaHora);

            // Configurar variables de salida
            bean.setFuncionId(funcionId);
            bean.setCodigoEstado(1);
            bean.setMensaje("Función registrada exitosamente");

            return bean;

        } catch (SQLException e) {
            bean.setCodigoEstado(0);
            bean.setMensaje("ERROR: " + e.getMessage());
            throw e;
        }
    }

    // ========== VALIDACIONES PARA PELÍCULA ==========

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarTituloPelicula(String titulo) throws SQLException {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new SQLException("El título de la película es obligatorio");
        }
        if (titulo.length() > 200) {
            throw new SQLException("El título no puede exceder los 200 caracteres");
        }
        if (titulo.trim().length() < 2) {
            throw new SQLException("El título debe tener al menos 2 caracteres");
        }

        // Validar que no exista una película activa con el mismo título
        String sql = "SELECT COUNT(1) FROM PELICULA WHERE UPPER(Titulo) = UPPER(?) AND Activa = 1";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, titulo.trim());
        if (count > 0) {
            throw new SQLException("Ya existe una película activa con el título: " + titulo);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarGeneroPelicula(String genero) throws SQLException {
        if (genero == null || genero.trim().isEmpty()) {
            throw new SQLException("El género de la película es obligatorio");
        }
        if (genero.length() > 50) {
            throw new SQLException("El género no puede exceder los 50 caracteres");
        }

        // Validar géneros comunes
        String[] generosValidos = {"Acción", "Aventura", "Comedia", "Drama", "Fantasía", "Terror",
                "Ciencia Ficción", "Romance", "Suspenso", "Animación", "Documental",
                "Familiar", "Musical", "Bélico", "Crimen", "Misterio", "Western"};
        boolean valido = false;
        for (String gen : generosValidos) {
            if (genero.equalsIgnoreCase(gen)) {
                valido = true;
                break;
            }
        }
        if (!valido) {
            throw new SQLException("Género no válido. Use: Acción, Aventura, Comedia, Drama, etc.");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarDuracionPelicula(int duracion) throws SQLException {
        if (duracion <= 0) {
            throw new SQLException("La duración debe ser mayor a 0 minutos");
        }
        if (duracion < 60) {
            throw new SQLException("La duración mínima es de 60 minutos");
        }
        if (duracion > 240) {
            throw new SQLException("La duración no puede exceder las 4 horas (240 minutos)");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarClasificacionPelicula(String clasificacion) throws SQLException {
        if (clasificacion == null || clasificacion.trim().isEmpty()) {
            throw new SQLException("La clasificación es obligatoria");
        }
        if (clasificacion.length() > 10) {
            throw new SQLException("La clasificación no puede exceder los 10 caracteres");
        }

        // Validar clasificaciones según normativa peruana
        String[] clasificacionesValidas = {"APT", "PG", "PG-13", "+14", "+18", "C"};
        boolean valida = false;
        for (String clasif : clasificacionesValidas) {
            if (clasificacion.equalsIgnoreCase(clasif)) {
                valida = true;
                break;
            }
        }
        if (!valida) {
            throw new SQLException("Clasificación no válida. Use: APT, PG, PG-13, +14, +18, C");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarDirectorPelicula(String director) throws SQLException {
        if (director == null || director.trim().isEmpty()) {
            throw new SQLException("El director es obligatorio");
        }
        if (director.length() > 100) {
            throw new SQLException("El nombre del director no puede exceder los 100 caracteres");
        }
        if (director.trim().length() < 3) {
            throw new SQLException("El nombre del director debe tener al menos 3 caracteres");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarSinopsisPelicula(String sinopsis) throws SQLException {
        if (sinopsis == null || sinopsis.trim().isEmpty()) {
            throw new SQLException("La sinopsis es obligatoria");
        }
        if (sinopsis.trim().length() < 10) {
            throw new SQLException("La sinopsis debe tener al menos 10 caracteres");
        }
        if (sinopsis.length() > 1000) {
            throw new SQLException("La sinopsis no puede exceder los 1000 caracteres");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarFechaEstreno(String fechaEstreno) throws SQLException {
        if (fechaEstreno == null || fechaEstreno.trim().isEmpty()) {
            throw new SQLException("La fecha de estreno es obligatoria");
        }

        try {
            java.sql.Date fecha = java.sql.Date.valueOf(fechaEstreno);
            java.sql.Date fechaActual = new java.sql.Date(System.currentTimeMillis());

            // Validar que no sea una fecha futura muy lejana (máximo 1 año)
            java.sql.Date fechaMaxima = java.sql.Date.valueOf(LocalDateTime.now().plusYears(1).toLocalDate());

            if (fecha.after(fechaMaxima)) {
                throw new SQLException("La fecha de estreno no puede ser más de 1 año en el futuro");
            }

            // Validar que no sea muy antigua (mínimo 1900)
            java.sql.Date fechaMinima = java.sql.Date.valueOf("1900-01-01");
            if (fecha.before(fechaMinima)) {
                throw new SQLException("La fecha de estreno no puede ser anterior a 1900");
            }

        } catch (IllegalArgumentException e) {
            throw new SQLException("Formato de fecha inválido. Use: YYYY-MM-DD");
        }
    }

    // ========== VALIDACIONES PARA FUNCIÓN ==========

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarPelicula(int peliculaId) throws SQLException {
        String sql = "SELECT COUNT(1) FROM PELICULA WHERE PeliculaID = ? AND Activa = 1";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, peliculaId);
        if (count == 0) {
            throw new SQLException("La película no existe o no está activa");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarSala(int salaId) throws SQLException {
        String sql = "SELECT COUNT(1) FROM SALA WHERE SalaID = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, salaId);
        if (count == 0) {
            throw new SQLException("La sala no existe");
        }

        // Validar que la sala tenga capacidad disponible
        String sqlCapacidad = "SELECT CapacidadDisponible FROM SALA WHERE SalaID = ?";
        Integer capacidad = jdbcTemplate.queryForObject(sqlCapacidad, Integer.class, salaId);
        if (capacidad == null || capacidad <= 0) {
            throw new SQLException("La sala no tiene capacidad disponible para programar funciones");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarEmpleado(int empleadoId) throws SQLException {
        String sql = "SELECT COUNT(1) FROM EMPLEADO WHERE EmpleadoID = ?";
        int count = jdbcTemplate.queryForObject(sql, Integer.class, empleadoId);
        if (count == 0) {
            throw new SQLException("El empleado no existe");
        }

        // Validar que el empleado tenga permisos para crear funciones
        String sqlCargo = "SELECT Cargo FROM EMPLEADO WHERE EmpleadoID = ?";
        String cargo = jdbcTemplate.queryForObject(sqlCargo, String.class, empleadoId);
        if (!"Administrador".equals(cargo) && !"Taquillero".equals(cargo)) {
            throw new SQLException("El empleado no tiene permisos para crear funciones");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarPrecioBase(double precioBase) throws SQLException {
        if (precioBase <= 0) {
            throw new SQLException("El precio base debe ser mayor a 0");
        }
        if (precioBase < 10) {
            throw new SQLException("El precio base mínimo es de S/ 10.00");
        }
        if (precioBase > 100) {
            throw new SQLException("El precio base no puede exceder los S/ 100.00");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarFechaHoraFutura(LocalDateTime fechaHora) throws SQLException {
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new SQLException("No se puede programar una función en el pasado");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarAnticipacionMinima(LocalDateTime fechaHora) throws SQLException {
        // Validar que sea al menos 2 horas en el futuro
        if (fechaHora.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new SQLException("La función debe programarse con al menos 2 horas de anticipación");
        }

        // Validar que no sea más de 3 meses en el futuro
        if (fechaHora.isAfter(LocalDateTime.now().plusMonths(3))) {
            throw new SQLException("No se pueden programar funciones con más de 3 meses de anticipación");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private void validarHorarioSala(int salaId, LocalDateTime fechaHoraInicio) throws SQLException {
        // Considerar que una función dura aproximadamente 3 horas
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusHours(3);

        String sql = "SELECT COUNT(1) FROM FUNCION WHERE SalaID = ? " +
                "AND ((FechaHoraInicio BETWEEN ? AND ?) " +
                "OR (DATEADD(HOUR, 3, FechaHoraInicio) BETWEEN ? AND ?))";

        int count = jdbcTemplate.queryForObject(sql, Integer.class,
                salaId,
                Timestamp.valueOf(fechaHoraInicio),
                Timestamp.valueOf(fechaHoraFin),
                Timestamp.valueOf(fechaHoraInicio),
                Timestamp.valueOf(fechaHoraFin));

        if (count > 0) {
            throw new SQLException("La sala ya tiene una función programada en ese horario");
        }

        // Validar horario comercial (8:00 AM - 11:00 PM)
        int hora = fechaHoraInicio.getHour();
        if (hora < 8 || hora > 23) {
            throw new SQLException("Las funciones solo pueden programarse entre 8:00 AM y 11:00 PM");
        }
    }

    // ========== OPERACIONES DE BASE DE DATOS ==========

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private int insertarPelicula(PeliculaDto bean) {
        String sql = "INSERT INTO PELICULA (Titulo, Genero, Duracion, Clasificacion, Director, Sinopsis, FechaEstreno, Activa) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 1); " +
                "SELECT SCOPE_IDENTITY();";

        return jdbcTemplate.queryForObject(sql, Integer.class,
                bean.getTitulo(),
                bean.getGenero(),
                bean.getDuracion(),
                bean.getClasificacion(),
                bean.getDirector(),
                bean.getSinopsis(),
                bean.getFechaEstreno());
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private int insertarFuncion(FuncionDto bean, LocalDateTime fechaHora) {
        String sql = "INSERT INTO FUNCION (PeliculaID, SalaID, FechaHoraInicio, PrecioBase, AsientosDisponibles, Estado, CreadoPor) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?); " +
                "SELECT SCOPE_IDENTITY();";

        return jdbcTemplate.queryForObject(sql, Integer.class,
                bean.getPeliculaId(),
                bean.getSalaId(),
                Timestamp.valueOf(fechaHora),
                bean.getPrecioBase(),
                bean.getAsientosDisponibles(),
                bean.getEstado(),
                bean.getCreadoPor());
    }

    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    private int obtenerCapacidadSala(int salaId) {
        String sql = "SELECT CapacidadDisponible FROM SALA WHERE SalaID = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, salaId);
    }

    // ========== MÉTODOS UTILITARIOS ==========

    private LocalDateTime convertirFechaHora(String fechaHoraStr) throws SQLException {
        try {
            String fechaHoraLimpia = fechaHoraStr.replace("T", " ");

            if (fechaHoraLimpia.contains("/")) {
                // Formato DD/MM/YYYY HH:MM:SS
                String[] partes = fechaHoraLimpia.split(" ");
                String[] fechaParts = partes[0].split("/");
                String fechaISO = fechaParts[2] + "-" + fechaParts[1] + "-" + fechaParts[0];
                return LocalDateTime.parse(fechaISO + "T" + partes[1]);
            } else {
                // Formato YYYY-MM-DD HH:MM:SS
                return LocalDateTime.parse(fechaHoraLimpia.replace(" ", "T"));
            }
        } catch (Exception e) {
            throw new SQLException("Formato de fecha/hora inválido. Use: '2024-01-20 18:00:00' o '20/01/2024 18:00:00'");
        }
    }

    // ========== MÉTODOS DE CONSULTA ==========

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public java.util.List<java.util.Map<String, Object>> obtenerCartelera() {
        String sql = "SELECT f.FuncionID, p.Titulo AS Pelicula, s.Nombre AS Sala, " +
                "f.FechaHoraInicio, f.PrecioBase, f.AsientosDisponibles, " +
                "f.Estado, s.TipoSala, e.Nombre + ' ' + e.Apellido AS CreadoPor " +
                "FROM FUNCION f " +
                "INNER JOIN PELICULA p ON f.PeliculaID = p.PeliculaID " +
                "INNER JOIN SALA s ON f.SalaID = s.SalaID " +
                "INNER JOIN EMPLEADO e ON f.CreadoPor = e.EmpleadoID " +
                "WHERE p.Activa = 1 AND f.Estado = 'Programada' " +
                "ORDER BY f.FechaHoraInicio";

        return jdbcTemplate.queryForList(sql);
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public java.util.List<java.util.Map<String, Object>> obtenerPeliculasActivas() {
        String sql = "SELECT PeliculaID, Titulo, Genero, Duracion, Clasificacion, Director, FechaEstreno " +
                "FROM PELICULA WHERE Activa = 1 ORDER BY Titulo";

        return jdbcTemplate.queryForList(sql);
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public java.util.List<java.util.Map<String, Object>> obtenerSalasDisponibles() {
        String sql = "SELECT SalaID, Nombre, TipoSala, CapacidadTotal, CapacidadDisponible, Descripcion " +
                "FROM SALA WHERE CapacidadDisponible > 0 ORDER BY SalaID";

        return jdbcTemplate.queryForList(sql);
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public java.util.List<java.util.Map<String, Object>> obtenerEmpleadosAutorizados() {
        String sql = "SELECT EmpleadoID, Nombre, Apellido, Cargo FROM EMPLEADO " +
                "WHERE Cargo IN ('Administrador', 'Taquillero') ORDER BY Cargo, Nombre";

        return jdbcTemplate.queryForList(sql);
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public java.util.List<java.util.Map<String, Object>> obtenerFuncionesPorPelicula(int peliculaId) {
        String sql = "SELECT f.FuncionID, f.FechaHoraInicio, s.Nombre AS Sala, " +
                "f.PrecioBase, f.AsientosDisponibles, f.Estado " +
                "FROM FUNCION f " +
                "INNER JOIN SALA s ON f.SalaID = s.SalaID " +
                "WHERE f.PeliculaID = ? AND f.Estado = 'Programada' " +
                "ORDER BY f.FechaHoraInicio";

        return jdbcTemplate.queryForList(sql, peliculaId);
    }
}