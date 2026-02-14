package pe.edu.uni.CineStarBarrio.dto;
import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString //Necesito que luego del pago me redirija a un apartado que sea de
public class BoletoDto {
    // Datos del Boleto
    private String codigoBoleto;
    private int reservaId;

    // Datos de Verificación
    private String clienteNombreCompleto;
    private double precioTotal;
    private String peliculaTitulo;
    private String salaNombre;
    private String fechaHoraFuncion;
    private List<String> asientos;
}