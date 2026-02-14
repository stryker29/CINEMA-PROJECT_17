package pe.edu.uni.CineStarBarrio.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmacionDto {
    private int reservaId;
    private int empleadoId; // El cajero que cobra
}
