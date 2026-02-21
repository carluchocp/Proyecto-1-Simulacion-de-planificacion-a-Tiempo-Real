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
public class FCFS implements AlgoritmoPlanificacion {

    @Override
    public Proceso seleccionarProceso(Cola<Proceso> colaListos) {
        return colaListos.desencolar();
    }

    @Override
    public boolean debePreemptar(Proceso enEjecucion, Cola<Proceso> colaListos) {
        return false;
    }

    @Override
    public boolean usaQuantum() {
        return false;
    }

    @Override
    public int getQuantum() {
        return 0;
    }

    @Override
    public PoliticaPlanificacion getPolitica() {
        return PoliticaPlanificacion.FCFS;
    }
}
