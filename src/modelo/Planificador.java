/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import estructuras.Nodo;
import planificadores.AlgoritmoPlanificacion;
import planificadores.PoliticaPlanificacion;
import planificadores.FCFS;
import planificadores.RoundRobin;
import planificadores.SRT;
import planificadores.Prioridad;
import planificadores.EDF;

/**
 *
 * @author carluchocp
 */
public class Planificador extends Thread {

    private Memoria memoria;
    private Reloj reloj;
    private CPU cpu1;
    private CPU cpu2;
    private boolean enEjecucion;
    private AlgoritmoPlanificacion algoritmo;

    public Planificador(Memoria memoria, Reloj reloj, CPU cpu1, CPU cpu2) {
        this.memoria = memoria;
        this.reloj = reloj;
        this.cpu1 = cpu1;
        this.cpu2 = cpu2;
        this.enEjecucion = false;
        this.algoritmo = new FCFS();
    }

    // ======================== Intercambio dinámico ========================

    public synchronized void cambiarAlgoritmo(AlgoritmoPlanificacion nuevoAlgoritmo) {
        System.out.println("[PLANIFICADOR] Cambiando de " + this.algoritmo.getPolitica()
                + " a " + nuevoAlgoritmo.getPolitica());
        this.algoritmo = nuevoAlgoritmo;
    }

    public synchronized void cambiarAlgoritmo(PoliticaPlanificacion politica, int quantum) {
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
    }

    public synchronized AlgoritmoPlanificacion getAlgoritmo() {
        return algoritmo;
    }

    public PoliticaPlanificacion getPoliticaActual() {
        return algoritmo.getPolitica();
    }

    // ======================== Hilo principal ========================

    @Override
    public void run() {
        this.enEjecucion = true;
        int cicloAnterior = reloj.getCicloGlobal();

        while (enEjecucion) {
            if (reloj.getCicloGlobal() > cicloAnterior) {
                cicloAnterior = reloj.getCicloGlobal();
                cicloplanificacion();
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                enEjecucion = false;
            }
        }
    }

    private synchronized void cicloplanificacion() {
        actualizarDeadlines();
        gestionarBloqueados();
        verificarPreemption(cpu1);
        verificarPreemption(cpu2);
        asignarProcesosACPUs();
        memoria.intentarReactivar();
    }

    // ======================== Gestión de deadlines ========================

    private void actualizarDeadlines() {
        // Decrementar deadline de todos los procesos en Listos
        decrementarDeadlineCola(memoria.getColaListos());
        // Decrementar deadline de Bloqueados
        decrementarDeadlineCola(memoria.getColaBloqueados());
        // Decrementar deadline de Suspendidos
        decrementarDeadlineCola(memoria.getColaListosSuspendidos());
        decrementarDeadlineCola(memoria.getColaBloqueadosSuspendidos());

        // Decrementar deadline de procesos en CPUs
        if (cpu1.getProcesoActual() != null) {
            cpu1.getProcesoActual().setTiempoRestanteDeadline(
                cpu1.getProcesoActual().getTiempoRestanteDeadline() - 1);
        }
        if (cpu2.getProcesoActual() != null) {
            cpu2.getProcesoActual().setTiempoRestanteDeadline(
                cpu2.getProcesoActual().getTiempoRestanteDeadline() - 1);
        }
    }

    private void decrementarDeadlineCola(estructuras.Cola<Proceso> cola) {
        Nodo<Proceso> actual = cola.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            p.setTiempoRestanteDeadline(p.getTiempoRestanteDeadline() - 1);
            if (p.deadlineExpirado()) {
                System.out.println("[DEADLINE] Fallo de Deadline en Proceso " + p.getId()
                    + " (" + p.getNombre() + ")");
            }
            actual = actual.getSiguiente();
        }
    }

    // ======================== Gestión de bloqueados ========================

    private void gestionarBloqueados() {
        // Decrementar ciclos de E/S restantes
        Nodo<Proceso> actual = memoria.getColaBloqueados().getPrimerNodo();
        while (actual != null) {
            actual.getContenido().setCiclosESRestantes(
                actual.getContenido().getCiclosESRestantes() - 1);
            actual = actual.getSiguiente();
        }

        // Mover los que terminaron E/S a la cola de listos
        estructuras.Cola<Proceso> sigueEnBloqueado = new estructuras.Cola<>();
        Proceso p;
        while ((p = memoria.getColaBloqueados().desencolar()) != null) {
            if (p.getCiclosESRestantes() <= 0) {
                p.setEstado(EstadoProceso.LISTO);
                memoria.getColaListos().encolar(p);
                System.out.println("[PLANIFICADOR] " + p.getId() + " desbloqueado -> Listo");
            } else {
                sigueEnBloqueado.encolar(p);
            }
        }
        while ((p = sigueEnBloqueado.desencolar()) != null) {
            memoria.getColaBloqueados().encolar(p);
        }
    }

    // ======================== Preemption ========================

    private void verificarPreemption(CPU cpu) {
        Proceso enCPU = cpu.getProcesoActual();
        if (enCPU == null) {
            return;
        }

        // Preemption por quantum (Round Robin)
        if (algoritmo.usaQuantum() && cpu.getCiclosEnQuantum() >= algoritmo.getQuantum()) {
            System.out.println("[PREEMPTION] Quantum expirado para " + enCPU.getId());
            cpu.preemptar();
            return;
        }

        // Preemption por algoritmo (SRT, Prioridad, EDF)
        if (algoritmo.debePreemptar(enCPU, memoria.getColaListos())) {
            System.out.println("[PREEMPTION] " + algoritmo.getPolitica()
                    + " desaloja a " + enCPU.getId());
            cpu.preemptar();
        }
    }

    // ======================== Asignación a CPUs ========================

    private void asignarProcesosACPUs() {
        if (cpu1.getProcesoActual() == null && !memoria.getColaListos().estaVacia()) {
            Proceso p = algoritmo.seleccionarProceso(memoria.getColaListos());
            if (p != null) {
                cpu1.asignarProceso(p);
            }
        }
        if (cpu2.getProcesoActual() == null && !memoria.getColaListos().estaVacia()) {
            Proceso p = algoritmo.seleccionarProceso(memoria.getColaListos());
            if (p != null) {
                cpu2.asignarProceso(p);
            }
        }
    }

    public void detener() {
        this.enEjecucion = false;
    }
}
