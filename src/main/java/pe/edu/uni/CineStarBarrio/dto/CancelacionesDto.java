package pe.edu.uni.CineStarBarrio.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CancelacionesDto {

    //Sección: Búsqueda y consulta
    private Integer reservaID;
    private String codigoReserva;
    private String estado;
    private String cliente;
    private Date fechaHoraInicio;
    private String pelicula;

    //Sección: Detalles del asiento
    private Integer asientoID;
    private String fila;
    private Integer numero;
    private String tipo;

    //Sección: Cancelación
    //Variables entrada
    private Integer empleadoID;
    private String motivo;

    //Variables salida
    private Integer estadoCancelar;// (1 = éxito, -1 = error)
    private String mensaje;
    private Date fechaCancelacion;      // Fecha en que se canceló
    private String empleadoNombre;      // Nombre del empleado que canceló
}
