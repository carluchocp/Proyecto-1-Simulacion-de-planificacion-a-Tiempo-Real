/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author diego
 */
public class CPU extends Thread {
    
    private int id;
    private Memoria memoria;
    private Reloj reloj;
    private Proceso procesoActual;
    private boolean enEjecucion;

    public CPU(int id, Memoria memoria, Reloj reloj) {
        this.id = id;
        this.memoria = memoria;
        this.reloj = reloj;
        this.enEjecucion = false;
    }

    @Override
    public void run() {
        this.enEjecucion = true;
        int cicloAnterior = reloj.getCicloGlobal();

        while (enEjecucion) {
            if (reloj.getCicloGlobal() > cicloAnterior) {
                cicloAnterior = reloj.getCicloGlobal();
                ejecutarCiclo();
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                enEjecucion = false;
            }
        }
    }

    private void ejecutarCiclo() {
        if (procesoActual == null) {
            procesoActual = memoria.desencolarListo();
            if (procesoActual != null) {
                procesoActual.setEstado(EstadoProceso.EJECUCION);
            }
        }

        if (procesoActual != null) {
            procesoActual.avanzarCiclo();

            if (procesoActual.haTerminado()) {
                procesoActual.setEstado(EstadoProceso.TERMINADO);
                memoria.getColaTerminados().encolar(procesoActual);
                procesoActual = null;
            } else if (procesoActual.necesitaES()) {
                procesoActual.setEstado(EstadoProceso.BLOQUEADO);
                memoria.encolarBloqueado(procesoActual);
                procesoActual = null;
            }
        }
    }

    public Proceso getProcesoActual() {
        return procesoActual;
    }

    public int getCpuId() {
        return id;
    }

    public void detener() {
        this.enEjecucion = false;
    }
}
