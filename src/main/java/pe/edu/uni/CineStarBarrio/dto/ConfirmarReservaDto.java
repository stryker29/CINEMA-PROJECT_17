package pe.edu.uni.CineStarBarrio.dto;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ConfirmarReservaDto {
    // Variables de entrada
    private int reservaId;
    private int empleadoId;

    // Variables de salida
    private String codigoBoleto;
    private String mensaje;
    private int codigoEstado;
}