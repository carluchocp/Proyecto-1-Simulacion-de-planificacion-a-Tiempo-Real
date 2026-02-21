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
    private volatile boolean pausado;
    private volatile boolean enInterrupcion;
    private int ciclosEnQuantum;

    public CPU(int id, Memoria memoria, Reloj reloj) {
        this.id = id;
        this.memoria = memoria;
        this.reloj = reloj;
        this.enEjecucion = false;
        this.pausado = true;
        this.enInterrupcion = false;
        this.ciclosEnQuantum = 0;
    }

    @Override
    public void run() {
        this.enEjecucion = true;
        int cicloAnterior = reloj.getCicloGlobal();

        while (enEjecucion) {
            if (!pausado && reloj.getCicloGlobal() > cicloAnterior) {
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
        if (procesoActual == null || enInterrupcion) {
            return;
        }

        procesoActual.avanzarCiclo();
        procesoActual.setMar(procesoActual.getPc());
        ciclosEnQuantum++;

        if (procesoActual.haTerminado()) {
            procesoActual.setEstado(EstadoProceso.TERMINADO);
            memoria.encolarTerminado(procesoActual);  // esto ahora decrementa procesosEnRAM
            System.out.println("[CPU-" + id + "] " + procesoActual.getId() + " TERMINADO");
            procesoActual = null;
            ciclosEnQuantum = 0;
            return;
        }

        if (procesoActual.necesitaES()) {
            procesoActual.setEstado(EstadoProceso.BLOQUEADO);
            procesoActual.setCiclosESRestantes(procesoActual.getDuracionES());
            memoria.encolarBloqueadoDirecto(procesoActual);  // sigue en RAM, no cambia contador
            System.out.println("[CPU-" + id + "] " + procesoActual.getId()
                    + " -> BLOQUEADO (E/S, " + procesoActual.getDuracionES() + " ciclos)");
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
            memoria.reEncolarListo(procesoActual);
            System.out.println("[CPU-" + id + "] Preemptado: " + procesoActual.getId());
            procesoActual = null;
            ciclosEnQuantum = 0;
        }
    }

    // ======================== Interrupciones ========================

    public synchronized Proceso interrumpir() {
        enInterrupcion = true;
        Proceso suspendido = procesoActual;
        if (suspendido != null) {
            suspendido.setEstado(EstadoProceso.LISTO);
            procesoActual = null;
            ciclosEnQuantum = 0;
        }
        return suspendido;
    }

    public synchronized void restaurarProceso(Proceso p) {
        p.setEstado(EstadoProceso.EJECUCION);
        this.procesoActual = p;
        this.ciclosEnQuantum = 0;
        this.enInterrupcion = false;
    }

    public synchronized void finalizarInterrupcion() {
        this.enInterrupcion = false;
    }

    public boolean isEnInterrupcion() { return enInterrupcion; }

    // ======================== Control ========================

    public void pausar() { this.pausado = true; }
    public void reanudar() { this.pausado = false; }
    public boolean isPausado() { return pausado; }

    public synchronized Proceso getProcesoActual() { return procesoActual; }
    public int getCpuId() { return id; }
    public int getCiclosEnQuantum() { return ciclosEnQuantum; }

    public void detener() { this.enEjecucion = false; }

    public synchronized void limpiar() {
        this.procesoActual = null;
        this.ciclosEnQuantum = 0;
        this.enInterrupcion = false;
        this.pausado = true;
    }
}
