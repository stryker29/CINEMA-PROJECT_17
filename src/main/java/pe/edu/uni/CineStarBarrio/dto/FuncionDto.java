package pe.edu.uni.CineStarBarrio.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class FuncionDto {

    private Integer funcionId;
    private Integer peliculaId;
    private Integer salaId;

    private String titulo;
    private String genero;
    private Integer duracion;
    private String clasificacion;

    private String fechaHoraInicio;   // Formato texto (yyyy-MM-dd HH:mm:ss)
    private String salaNombre;

    private Integer asientosDisponibles;
    private double precioBase;
    private int creadoPor;

    private String estado;
    private String mensaje;
    private int codigoEstado;
}
