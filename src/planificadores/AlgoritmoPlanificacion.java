/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package planificadores;

import estructuras.Cola;
import modelo.Proceso;

/**
 *
 * @author carluchocp
 */
public interface AlgoritmoPlanificacion {

    Proceso seleccionarProceso(Cola<Proceso> colaListos);

    boolean debePreemptar(Proceso enEjecucion, Cola<Proceso> colaListos);

    boolean usaQuantum();

    int getQuantum();

    PoliticaPlanificacion getPolitica();
}
