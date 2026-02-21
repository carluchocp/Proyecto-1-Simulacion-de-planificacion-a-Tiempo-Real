/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import java.util.concurrent.Semaphore;

/**
 * 5.4 — CPU sincronizada con el reloj mediante Semaphore.
 * 5.3 — Semáforo binario para exclusión mutua sobre procesoActual.
 *
 * @author carluchocp
 */
public class CPU extends Thread {

    private int id;
    private Memoria memoria;
    private Reloj reloj;
    private Planificador planificador;
    private volatile Proceso procesoActual;
    private volatile boolean enEjecucion;
    private volatile boolean pausado;
    private volatile boolean enInterrupcion;
    private int ciclosEnQuantum;

    // 5.3 — Semáforo binario para exclusión mutua sobre el estado de la CPU
    private final Semaphore mutexCpu = new Semaphore(1, true);

    // 5.4 — Referencia al semáforo del reloj (se inyecta según el id de CPU)
    private Semaphore semReloj;

    public CPU(int id, Memoria memoria, Reloj reloj) {
        this.id = id;
        this.memoria = memoria;
        this.reloj = reloj;
        this.enEjecucion = false;
        this.pausado = true;
        this.enInterrupcion = false;
        this.ciclosEnQuantum = 0;
        this.setName("CPU-" + id);
        this.setDaemon(true);
    }

    /**
     * 5.4 — Inyectar el semáforo del reloj correspondiente a esta CPU.
     * Debe llamarse ANTES de start().
     */
    public void setSemReloj(Semaphore semReloj) {
        this.semReloj = semReloj;
    }

    public void setPlanificador(Planificador planificador) {
        this.planificador = planificador;
    }

    // ======================== 5.4 — Hilo sincronizado con el reloj ========================

    @Override
    public void run() {
        this.enEjecucion = true;

        while (enEjecucion) {
            try {
                // 5.4 — Esperar exactamente un tick del reloj
                semReloj.acquire();
            } catch (InterruptedException e) {
                enEjecucion = false;
                break;
            }

            if (!enEjecucion) break;

            // Solo ejecutar si no está pausado
            if (!pausado) {
                ejecutarCiclo();
            }
        }
    }

    private void ejecutarCiclo() {
        mutexCpu.acquireUninterruptibly();
        try {
            if (procesoActual == null || enInterrupcion) {
                return;
            }

            // 5.2 — Delegar la ejecución al hilo del proceso
            procesoActual.ejecutarUnCiclo();
            ciclosEnQuantum++;

            if (procesoActual.haTerminado()) {
                procesoActual.setEstado(EstadoProceso.TERMINADO);
                procesoActual.detenerHilo();
                memoria.encolarTerminado(procesoActual);
                System.out.println("[CPU-" + id + "] " + procesoActual.getId() + " TERMINADO");
                planificador.registrarProcesoTerminado(procesoActual);
                procesoActual = null;
                ciclosEnQuantum = 0;
                return;
            }

            if (procesoActual.necesitaES()) {
                procesoActual.setEstado(EstadoProceso.BLOQUEADO);
                procesoActual.setCiclosESRestantes(procesoActual.getDuracionES());
                memoria.encolarBloqueadoDirecto(procesoActual);
                System.out.println("[CPU-" + id + "] " + procesoActual.getId()
                        + " -> BLOQUEADO (E/S, " + procesoActual.getDuracionES() + " ciclos)");
                procesoActual = null;
                ciclosEnQuantum = 0;
            }
        } finally {
            mutexCpu.release();
        }
    }

    // ======================== Asignación y Preemption ========================

    public void asignarProceso(Proceso p) {
        mutexCpu.acquireUninterruptibly();
        try {
            p.setEstado(EstadoProceso.EJECUCION);
            this.procesoActual = p;
            this.ciclosEnQuantum = 0;
            System.out.println("[CPU-" + id + "] Ejecutando: " + p.getId());
        } finally {
            mutexCpu.release();
        }
    }

    public void preemptar() {
        mutexCpu.acquireUninterruptibly();
        try {
            if (procesoActual != null) {
                procesoActual.setEstado(EstadoProceso.LISTO);
                memoria.reEncolarListo(procesoActual);
                System.out.println("[CPU-" + id + "] Preemptado: " + procesoActual.getId());
                procesoActual = null;
                ciclosEnQuantum = 0;
            }
        } finally {
            mutexCpu.release();
        }
    }

    // ======================== Interrupciones ========================

    /**
     * 6.4 — Una interrupción suspende el proceso actual.
     * El proceso se retira de la CPU sin cambiar su estado ni re-encolarlo;
     * la Interrupcion se encarga de restaurarlo o re-encolarlo al terminar.
     */
    public Proceso interrumpir() {
        mutexCpu.acquireUninterruptibly();
        try {
            enInterrupcion = true;
            Proceso suspendido = procesoActual;
            procesoActual = null;
            ciclosEnQuantum = 0;
            return suspendido;
        } finally {
            mutexCpu.release();
        }
    }

    public void restaurarProceso(Proceso p) {
        mutexCpu.acquireUninterruptibly();
        try {
            if (p.getEstado() != EstadoProceso.EJECUCION) {
                p.setEstado(EstadoProceso.EJECUCION);
            }
            this.procesoActual = p;
            this.ciclosEnQuantum = 0;
            this.enInterrupcion = false;
        } finally {
            mutexCpu.release();
        }
    }

    public void finalizarInterrupcion() {
        mutexCpu.acquireUninterruptibly();
        try {
            this.enInterrupcion = false;
        } finally {
            mutexCpu.release();
        }
    }

    public boolean isEnInterrupcion() { return enInterrupcion; }

    // ======================== Control ========================

    public void pausar() { this.pausado = true; }
    public void reanudar() { this.pausado = false; }
    public boolean isPausado() { return pausado; }

    public Proceso getProcesoActual() {
        mutexCpu.acquireUninterruptibly();
        try {
            return procesoActual;
        } finally {
            mutexCpu.release();
        }
    }

    public int getCpuId() { return id; }
    public int getCiclosEnQuantum() { return ciclosEnQuantum; }

    public void detener() { this.enEjecucion = false; }

    public void limpiar() {
        mutexCpu.acquireUninterruptibly();
        try {
            if (procesoActual != null) {
                procesoActual.detenerHilo();
            }
            this.procesoActual = null;
            this.ciclosEnQuantum = 0;
            this.enInterrupcion = false;
            this.pausado = true;
        } finally {
            mutexCpu.release();
        }
    }
}
