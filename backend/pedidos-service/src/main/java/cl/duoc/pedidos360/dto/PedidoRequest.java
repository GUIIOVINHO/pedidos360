package cl.duoc.pedidos360.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PedidoRequest {

    @NotBlank(message = "El cliente es obligatorio")
    private String cliente;

    @NotBlank(message = "El producto es obligatorio")
    private String producto;

    @NotNull
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    private String estado;

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
