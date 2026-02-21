/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package modelo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author diego
 */

public enum EstadoProceso {
    NUEVO,
    LISTO,
    EJECUCION,
    BLOQUEADO,
    TERMINADO,
    LISTO_SUSPENDIDO,
    BLOQUEADO_SUSPENDIDO;

    private static final Map<EstadoProceso, List<EstadoProceso>> transicionesValidas = new HashMap<>();

    static {
        transicionesValidas.put(NUEVO, Arrays.asList(LISTO));
        transicionesValidas.put(LISTO, Arrays.asList(EJECUCION, LISTO_SUSPENDIDO));
        transicionesValidas.put(EJECUCION, Arrays.asList(LISTO, BLOQUEADO, TERMINADO));
        transicionesValidas.put(BLOQUEADO, Arrays.asList(LISTO, BLOQUEADO_SUSPENDIDO));
        transicionesValidas.put(TERMINADO, Arrays.asList());
        transicionesValidas.put(LISTO_SUSPENDIDO, Arrays.asList(LISTO));
        transicionesValidas.put(BLOQUEADO_SUSPENDIDO, Arrays.asList(LISTO_SUSPENDIDO, BLOQUEADO));
    }

    public boolean puedeTransicionarA(EstadoProceso destino) {
        List<EstadoProceso> permitidos = transicionesValidas.get(this);
        return permitidos != null && permitidos.contains(destino);
    }
}
