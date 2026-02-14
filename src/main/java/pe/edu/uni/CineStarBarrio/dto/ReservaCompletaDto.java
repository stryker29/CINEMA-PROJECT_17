package pe.edu.uni.CineStarBarrio.dto;

import lombok.*;
import java.util.List;

/**
 * DTO Unificado para todo el proceso de reserva
 * Maneja: Cliente, Función, Asientos y Confirmación
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaCompletaDto {

    // ==================== DATOS DE ENTRADA ====================

    // Cliente
    private Integer idCliente;

    // Función
    private Integer idFuncion;

    // Empleado que registra
    private Integer idEmpleado;

    // Asientos seleccionados
    private List<AsientoSeleccionado> asientosSeleccionados;

    // ==================== DATOS DE SALIDA ====================

    private String codigoReserva;
    private Integer cantidadEntradas;
    private Double precioTotal;
    private Integer capacidadInicial;
    private Integer capacidadFinal;
    private String fechaReserva;
    private String fechaExpiracion;
    private String estado;
    private String mensaje;
    private Boolean success;

    // ==================== CLASE INTERNA: ASIENTO ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AsientoSeleccionado {
        private String fila;
        private Integer numero;
        private Integer tipoEntradaId;

        // Campos calculados/internos
        private Integer asientoId;
        private String tipoAsiento;
        private String estadoAsiento;
        private Double precio;
        private String nombreTipoEntrada;

        public String getUbicacion() {
            return fila + numero;
        }
    }
}