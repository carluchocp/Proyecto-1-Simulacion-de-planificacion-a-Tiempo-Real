/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import estructuras.Cola;
import estructuras.Nodo;
import planificadores.AlgoritmoPlanificacion;
import planificadores.PoliticaPlanificacion;
import planificadores.FCFS;
import planificadores.RoundRobin;
import planificadores.SRT;
import planificadores.Prioridad;
import planificadores.EDF;
import java.util.concurrent.Semaphore;

/**
 * 5.4 — Planificador sincronizado con el reloj vía Semaphore.
 * 5.3 — Semáforo para exclusión mutua sobre el algoritmo.
 *
 * @author carluchocp
 */
public class Planificador extends Thread {

    private Memoria memoria;
    private Reloj reloj;
    private CPU cpu1;
    private CPU cpu2;
    private volatile boolean enEjecucion;
    private volatile boolean pausado;
    private AlgoritmoPlanificacion algoritmo;
    private Runnable onSimulacionCompletada;

    // 5.3 — Semáforo para exclusión mutua sobre el algoritmo
    private final Semaphore mutexAlgoritmo = new Semaphore(1, true);

    public Planificador(Memoria memoria, Reloj reloj, CPU cpu1, CPU cpu2) {
        this.memoria = memoria;
        this.reloj = reloj;
        this.cpu1 = cpu1;
        this.cpu2 = cpu2;
        this.enEjecucion = false;
        this.pausado = true;
        this.algoritmo = new FCFS();
        this.setName("Planificador");
        this.setDaemon(true);
    }

    public void setOnSimulacionCompletada(Runnable callback) {
        this.onSimulacionCompletada = callback;
    }

    // ======================== Intercambio dinámico ========================

    public void cambiarAlgoritmo(AlgoritmoPlanificacion nuevoAlgoritmo) {
        mutexAlgoritmo.acquireUninterruptibly();
        try {
            System.out.println("[PLANIFICADOR] Cambiando de " + this.algoritmo.getPolitica()
                    + " a " + nuevoAlgoritmo.getPolitica());
            this.algoritmo = nuevoAlgoritmo;
        } finally {
            mutexAlgoritmo.release();
        }
    }

    public void cambiarAlgoritmo(PoliticaPlanificacion politica, int quantum) {
        mutexAlgoritmo.acquireUninterruptibly();
        try {
            switch (politica) {
                case FCFS:
                    this.algoritmo = new FCFS();
                    break;
                case ROUND_ROBIN:
                    this.algoritmo = new RoundRobin(quantum);
                    break;
                case SRT:
                    this.algoritmo = new SRT();
                    break;
                case PRIORIDAD:
                    this.algoritmo = new Prioridad();
                    break;
                case EDF:
                    this.algoritmo = new EDF();
                    break;
            }
            System.out.println("[PLANIFICADOR] Algoritmo cambiado a: " + politica.getDescripcion());
        } finally {
            mutexAlgoritmo.release();
        }
    }

    public AlgoritmoPlanificacion getAlgoritmo() {
        mutexAlgoritmo.acquireUninterruptibly();
        try {
            return algoritmo;
        } finally {
            mutexAlgoritmo.release();
        }
    }

    public PoliticaPlanificacion getPoliticaActual() {
        mutexAlgoritmo.acquireUninterruptibly();
        try {
            return algoritmo.getPolitica();
        } finally {
            mutexAlgoritmo.release();
        }
    }

    // ======================== Control ========================

    public void pausar() { this.pausado = true; }
    public void reanudar() { this.pausado = false; }
    public boolean isPausado() { return pausado; }

    // ======================== 5.4 — Hilo sincronizado con el reloj ========================

    @Override
    public void run() {
        this.enEjecucion = true;

        while (enEjecucion) {
            try {
                // 5.4 — Esperar un tick del reloj
                reloj.getSemPlanificador().acquire();
            } catch (InterruptedException e) {
                enEjecucion = false;
                break;
            }

            if (!enEjecucion) break;

            if (!pausado) {
                cicloPlanificacion();
            }
        }
    }

    private void cicloPlanificacion() {
        mutexAlgoritmo.acquireUninterruptibly();
        try {
            admitirNuevos();
            actualizarDeadlines();
            gestionarBloqueados();
            gestionarBloqueadosSuspendidos();
            memoria.intentarReactivar();
            verificarPreemption(cpu1);
            verificarPreemption(cpu2);
            asignarProcesosACPUs();
            memoria.intentarReactivar();
            verificarSimulacionCompletada();
        } finally {
            mutexAlgoritmo.release();
        }
    }

    // ======================== Admisión de nuevos ========================

    private void admitirNuevos() {
        while (!memoria.getColaNuevos().estaVacia()) {
            Proceso p = memoria.getColaNuevos().desencolar();
            if (p != null) {
                memoria.admitirProceso(p);
                System.out.println("[PLANIFICADOR] Admitido: " + p.getId() + " -> " + p.getEstado());
            }
        }
    }

    // ======================== Gestión de deadlines ========================

    private void actualizarDeadlines() {
        decrementarDeadlineCola(memoria.getColaListos());
        decrementarDeadlineCola(memoria.getColaBloqueados());
        decrementarDeadlineCola(memoria.getColaListosSuspendidos());
        decrementarDeadlineCola(memoria.getColaBloqueadosSuspendidos());

        if (cpu1.getProcesoActual() != null) {
            cpu1.getProcesoActual().setTiempoRestanteDeadline(
                cpu1.getProcesoActual().getTiempoRestanteDeadline() - 1);
        }
        if (cpu2.getProcesoActual() != null) {
            cpu2.getProcesoActual().setTiempoRestanteDeadline(
                cpu2.getProcesoActual().getTiempoRestanteDeadline() - 1);
        }
    }

    private void decrementarDeadlineCola(Cola<Proceso> cola) {
        Nodo<Proceso> actual = cola.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            p.setTiempoRestanteDeadline(p.getTiempoRestanteDeadline() - 1);
            if (p.deadlineExpirado()) {
                System.out.println("[DEADLINE] Fallo en Proceso " + p.getId());
            }
            actual = actual.getSiguiente();
        }
    }

    // ======================== Gestión de bloqueados ========================

    private void gestionarBloqueados() {
        Nodo<Proceso> actual = memoria.getColaBloqueados().getPrimerNodo();
        while (actual != null) {
            actual.getContenido().setCiclosESRestantes(
                actual.getContenido().getCiclosESRestantes() - 1);
            actual = actual.getSiguiente();
        }

        Cola<Proceso> sigueEnBloqueado = new Cola<>();
        Proceso p;
        while ((p = memoria.getColaBloqueados().desencolar()) != null) {
            if (p.getCiclosESRestantes() <= 0) {
                p.setEstado(EstadoProceso.LISTO);
                memoria.getColaListos().encolar(p);
                System.out.println("[PLANIFICADOR] " + p.getId()
                        + " E/S completada -> Listo");
            } else {
                sigueEnBloqueado.encolar(p);
            }
        }
        while ((p = sigueEnBloqueado.desencolar()) != null) {
            memoria.getColaBloqueados().encolar(p);
        }

        while (memoria.ramLlena() && !memoria.getColaBloqueados().estaVacia()) {
            memoria.suspenderMenosUrgente();
        }
    }

    // ======================== Gestión de bloqueados suspendidos ========================

    private void gestionarBloqueadosSuspendidos() {
        Nodo<Proceso> actual = memoria.getColaBloqueadosSuspendidos().getPrimerNodo();
        while (actual != null) {
            actual.getContenido().setCiclosESRestantes(
                actual.getContenido().getCiclosESRestantes() - 1);
            actual = actual.getSiguiente();
        }

        Cola<Proceso> sigueEnBloqSusp = new Cola<>();
        Proceso p;
        while ((p = memoria.getColaBloqueadosSuspendidos().desencolar()) != null) {
            if (p.getCiclosESRestantes() <= 0) {
                p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
                memoria.getColaListosSuspendidos().encolar(p);
                System.out.println("[PLANIFICADOR] " + p.getId()
                        + " E/S completada (suspendido) -> Listo/Suspendido");
            } else {
                sigueEnBloqSusp.encolar(p);
            }
        }
        while ((p = sigueEnBloqSusp.desencolar()) != null) {
            memoria.getColaBloqueadosSuspendidos().encolar(p);
        }
    }

    // ======================== Detección de fin ========================

    private void verificarSimulacionCompletada() {
        boolean nuevosVacia = memoria.getColaNuevos().estaVacia();
        boolean listosVacia = memoria.getColaListos().estaVacia();
        boolean bloqueadosVacia = memoria.getColaBloqueados().estaVacia();
        boolean listosSuspVacia = memoria.getColaListosSuspendidos().estaVacia();
        boolean bloqSuspVacia = memoria.getColaBloqueadosSuspendidos().estaVacia();
        boolean cpu1Libre = cpu1.getProcesoActual() == null && !cpu1.isEnInterrupcion();
        boolean cpu2Libre = cpu2.getProcesoActual() == null && !cpu2.isEnInterrupcion();
        boolean hayTerminados = memoria.getColaTerminados().getTamano() > 0;

        if (nuevosVacia && listosVacia && bloqueadosVacia
                && listosSuspVacia && bloqSuspVacia
                && cpu1Libre && cpu2Libre && hayTerminados) {
            System.out.println("[PLANIFICADOR] *** SIMULACION COMPLETADA ***");
            if (onSimulacionCompletada != null) {
                onSimulacionCompletada.run();
            }
        }
    }

    // ======================== Preemption ========================

    private void verificarPreemption(CPU cpu) {
        Proceso enCPU = cpu.getProcesoActual();
        if (enCPU == null || cpu.isEnInterrupcion()) return;

        if (algoritmo.usaQuantum() && cpu.getCiclosEnQuantum() >= algoritmo.getQuantum()) {
            System.out.println("[PREEMPTION] Quantum expirado para " + enCPU.getId());
            cpu.preemptar();
            return;
        }

        if (algoritmo.debePreemptar(enCPU, memoria.getColaListos())) {
            System.out.println("[PREEMPTION] " + algoritmo.getPolitica() + " desaloja a " + enCPU.getId());
            cpu.preemptar();
        }
    }

    // ======================== Asignación a CPUs ========================

    private void asignarProcesosACPUs() {
        if (cpu1.getProcesoActual() == null && !cpu1.isEnInterrupcion()
                && !memoria.getColaListos().estaVacia()) {
            Proceso p = algoritmo.seleccionarProceso(memoria.getColaListos());
            if (p != null) cpu1.asignarProceso(p);
        }
        if (cpu2.getProcesoActual() == null && !cpu2.isEnInterrupcion()
                && !memoria.getColaListos().estaVacia()) {
            Proceso p = algoritmo.seleccionarProceso(memoria.getColaListos());
            if (p != null) cpu2.asignarProceso(p);
        }
    }

    public void detener() { this.enEjecucion = false; }
}
