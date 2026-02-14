package pe.edu.uni.CineStarBarrio.dto;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class VentaDto {
    // Datos del Cliente
    private String clienteNombre;
    private String clienteApellido;
    private String clienteTelefono;

    // Datos de la Venta
    private int funcionId;
    private int empleadoId;
    private List<Integer> asientosIds;
}