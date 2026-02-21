/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author carluchocp
 */
public class CPU extends Thread {

    private int id;
    private Memoria memoria;
    private Reloj reloj;
    private volatile Proceso procesoActual;
    private volatile boolean enEjecucion;
    private int ciclosEnQuantum;

    public CPU(int id, Memoria memoria, Reloj reloj) {
        this.id = id;
        this.memoria = memoria;
        this.reloj = reloj;
        this.enEjecucion = false;
        this.ciclosEnQuantum = 0;
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

    private synchronized void ejecutarCiclo() {
        if (procesoActual == null) {
            return; // El Planificador se encarga de asignar
        }

        // Ejecutar una instrucción
        procesoActual.avanzarCiclo();
        procesoActual.setMar(procesoActual.getPc());
        ciclosEnQuantum++;

        // ¿Terminó?
        if (procesoActual.haTerminado()) {
            procesoActual.setEstado(EstadoProceso.TERMINADO);
            memoria.encolarTerminado(procesoActual);
            System.out.println("[CPU-" + id + "] " + procesoActual.getId() + " TERMINADO");
            procesoActual = null;
            ciclosEnQuantum = 0;
            return;
        }

        // ¿Necesita E/S?
        if (procesoActual.necesitaES()) {
            procesoActual.setEstado(EstadoProceso.BLOQUEADO);
            procesoActual.setCiclosESRestantes(procesoActual.getCiclosParaES());
            memoria.encolarBloqueadoDirecto(procesoActual);
            System.out.println("[CPU-" + id + "] " + procesoActual.getId() + " -> BLOQUEADO (E/S)");
            procesoActual = null;
            ciclosEnQuantum = 0;
        }
    }

    // ======================== Asignación y Preemption ========================

    public synchronized void asignarProceso(Proceso p) {
        p.setEstado(EstadoProceso.EJECUCION);
        this.procesoActual = p;
        this.ciclosEnQuantum = 0;
        System.out.println("[CPU-" + id + "] Ejecutando: " + p.getId());
    }

    public synchronized void preemptar() {
        if (procesoActual != null) {
            procesoActual.setEstado(EstadoProceso.LISTO);
            memoria.getColaListos().encolar(procesoActual);
            System.out.println("[CPU-" + id + "] Preemptado: " + procesoActual.getId());
            procesoActual = null;
            ciclosEnQuantum = 0;
        }
    }

    // ======================== Getters ========================

    public synchronized Proceso getProcesoActual() {
        return procesoActual;
    }

    public int getCpuId() {
        return id;
    }

    public int getCiclosEnQuantum() {
        return ciclosEnQuantum;
    }

    public void detener() {
        this.enEjecucion = false;
    }
}
