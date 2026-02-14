package pe.edu.uni.CineStarBarrio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pe.edu.uni.CineStarBarrio.dto.FuncionDto;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FuncionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FuncionDto obtenerPorId(Integer funcionId) throws EmptyResultDataAccessException {
        String sql = """
                SELECT  f.FuncionID,
                        f.PeliculaID,
                        f.SalaID,
                        f.FechaHoraInicio,
                        f.AsientosDisponibles,
                        p.Titulo,
                        p.Genero,
                        p.Duracion,
                        p.Clasificacion,
                        s.Nombre AS SalaNombre
                FROM FUNCION f
                INNER JOIN PELICULA p ON p.PeliculaID = f.PeliculaID
                INNER JOIN SALA s ON s.SalaID = f.SalaID
                WHERE f.FuncionID = ?
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            FuncionDto dto = new FuncionDto();
            dto.setFuncionId(rs.getInt("FuncionID"));
            dto.setPeliculaId(rs.getInt("PeliculaID"));
            dto.setSalaId(rs.getInt("SalaID"));

            dto.setTitulo(rs.getString("Titulo"));
            dto.setGenero(rs.getString("Genero"));
            dto.setDuracion((Integer) rs.getObject("Duracion"));
            dto.setClasificacion(rs.getString("Clasificacion"));

            Timestamp ts = rs.getTimestamp("FechaHoraInicio");
            if (ts != null) {
                LocalDateTime ldt = ts.toLocalDateTime();
                dto.setFechaHoraInicio(ldt.format(FORMATTER));
            }

            dto.setSalaNombre(rs.getString("SalaNombre"));
            dto.setAsientosDisponibles((Integer) rs.getObject("AsientosDisponibles"));
            return dto;
        }, funcionId);
    }
}
