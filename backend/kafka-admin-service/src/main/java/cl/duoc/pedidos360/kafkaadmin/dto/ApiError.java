package cl.duoc.pedidos360.kafkaadmin.dto;

import java.util.List;

/** Cuerpo estandar de error de la API. */
public record ApiError(int status, String error, List<String> mensajes) {
}
