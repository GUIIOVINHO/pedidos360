package cl.duoc.pedidos360.kafkaadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;

import java.util.List;
import java.util.Map;

/** Modificacion de configuraciones de un topico: "set" asigna valores y "remove" vuelve al valor por defecto. */
public class TopicConfigUpdateRequest {

    private Map<String, String> set;
    private List<String> remove;

    @JsonIgnore
    @AssertTrue(message = "Debe indicar al menos una configuracion en 'set' o en 'remove'")
    public boolean isContieneCambios() {
        boolean hayAsignaciones = set != null && !set.isEmpty();
        boolean hayEliminaciones = remove != null && !remove.isEmpty();
        return hayAsignaciones || hayEliminaciones;
    }

    public Map<String, String> getSet() { return set; }
    public void setSet(Map<String, String> set) { this.set = set; }

    public List<String> getRemove() { return remove; }
    public void setRemove(List<String> remove) { this.remove = remove; }
}
