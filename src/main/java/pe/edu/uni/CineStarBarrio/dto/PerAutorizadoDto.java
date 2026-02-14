package pe.edu.uni.CineStarBarrio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerAutorizadoDto {

    // Sección: Datos de entrada (Login)
    private String usuario;
    private String clave;

    // Sección: Datos de salida (Respuesta)
    private Integer empleadoID;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String cargo;
    private Date fechaContratacion;

    // Sección: Control de respuesta
    private Integer estadoLogin; // 1 = éxito, -1 = error
    private String mensaje;
    private String token; // Opcional: para JWT
}