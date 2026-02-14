package pe.edu.uni.CineStarBarrio.dto;

import lombok.*;

/**
 * DTO para operaciones de cliente (registro, búsqueda)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteDto {

    // Datos de entrada (registro)
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;

    // Datos de salida (búsqueda)
    private Integer usuarioID;
    private Boolean activo;

    // Respuesta
    private Boolean success;
    private String mensaje;
}