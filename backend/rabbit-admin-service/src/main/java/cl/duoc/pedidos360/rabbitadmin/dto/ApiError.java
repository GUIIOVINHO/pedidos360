package cl.duoc.pedidos360.rabbitadmin.dto;

import java.util.List;

/** Cuerpo estandar de error de la API (mensajes claros, sin detalles internos). */
public record ApiError(int status, String error, List<String> mensajes) {
}
