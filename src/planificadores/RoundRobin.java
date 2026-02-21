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
public class RoundRobin implements AlgoritmoPlanificacion {

    private int quantum;

    public RoundRobin(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public Proceso seleccionarProceso(Cola<Proceso> colaListos) {
        return colaListos.desencolar();
    }

    @Override
    public boolean debePreemptar(Proceso enEjecucion, Cola<Proceso> colaListos) {
        return false; // La preemption por quantum se maneja en CPU con el contador
    }

    @Override
    public boolean usaQuantum() {
        return true;
    }

    @Override
    public int getQuantum() {
        return quantum;
    }

    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }

    @Override
    public PoliticaPlanificacion getPolitica() {
        return PoliticaPlanificacion.ROUND_ROBIN;
    }
}
