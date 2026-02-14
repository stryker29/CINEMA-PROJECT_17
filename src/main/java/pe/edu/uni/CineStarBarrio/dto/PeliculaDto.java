package pe.edu.uni.CineStarBarrio.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PeliculaDto {
    //Variables de entrada
    private String titulo;
    private String genero;
    private int duracion;
    private String clasificacion;
    private String director;
    private String sinopsis;
    private String fechaEstreno;
    private int creadoPor;
    //Variables de salida
    private int peliculaId;
    private boolean activa;
    private String mensaje;
    private int estado;


}