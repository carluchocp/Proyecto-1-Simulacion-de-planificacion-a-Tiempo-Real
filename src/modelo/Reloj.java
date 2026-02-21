/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import java.util.concurrent.Semaphore;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 5.1 — El Reloj corre en su propio Thread y notifica a los
 * componentes del sistema mediante semáforos en cada tick.
 *
 * @author carluchocp
 */
public class Reloj extends Thread {

    private volatile int cicloGlobal;
    private volatile int duracionCiclo; // en ms
    private volatile boolean enEjecucion;
    private volatile boolean pausado;

    // 5.4 — Semáforos de sincronización: uno por cada componente que
    //       necesita ejecutar exactamente una vez por tick del reloj.
    private final Semaphore semCpu1 = new Semaphore(0);
    private final Semaphore semCpu2 = new Semaphore(0);
    private final Semaphore semPlanificador = new Semaphore(0);
    private final Semaphore semGeneradorInt = new Semaphore(0);

    // Semáforos dinámicos para ISRs activas (cada interrupción registra uno)
    private final CopyOnWriteArrayList<Semaphore> semISRs = new CopyOnWriteArrayList<>();

    public Reloj() {
        this.cicloGlobal = 0;
        this.duracionCiclo = 500;
        this.enEjecucion = false;
        this.pausado = true;
        this.setName("Reloj-Sistema");
        this.setDaemon(true);
    }

    @Override
    public void run() {
        this.enEjecucion = true;
        while (enEjecucion) {
            try {
                Thread.sleep(duracionCiclo);
                if (!pausado) {
                    cicloGlobal++;
                    // Liberar un permit para cada componente sincronizado
                    semCpu1.release();
                    semCpu2.release();
                    semPlanificador.release();
                    semGeneradorInt.release();
                    // Liberar permits para todas las ISRs activas
                    for (Semaphore semISR : semISRs) {
                        semISR.release();
                    }
                }
            } catch (InterruptedException e) {
                enEjecucion = false;
            }
        }
    }

    /**
     * Registra un semáforo de ISR para que reciba ticks del reloj.
     * La interrupción lo llama al iniciar su run().
     */
    public Semaphore registrarISR() {
        Semaphore sem = new Semaphore(0);
        semISRs.add(sem);
        return sem;
    }

    /**
     * Desregistra un semáforo de ISR cuando la interrupción termina.
     */
    public void desregistrarISR(Semaphore sem) {
        semISRs.remove(sem);
    }

    // --- Getters de semáforos para inyectar en CPU y Planificador ---
    public Semaphore getSemCpu1() { return semCpu1; }
    public Semaphore getSemCpu2() { return semCpu2; }
    public Semaphore getSemPlanificador() { return semPlanificador; }
    public Semaphore getSemGeneradorInt() { return semGeneradorInt; }

    public int getCicloGlobal() { return cicloGlobal; }
    public int getDuracionCiclo() { return duracionCiclo; }
    public void setDuracionCiclo(int duracionCiclo) { this.duracionCiclo = duracionCiclo; }

    public void pausar() { this.pausado = true; }
    public void reanudar() { this.pausado = false; }
    public boolean isPausado() { return pausado; }

    public void detener() { this.enEjecucion = false; }

    public void reiniciar() {
        this.cicloGlobal = 0;
        this.pausado = true;
        // Drenar permits pendientes para evitar ejecuciones fantasma
        semCpu1.drainPermits();
        semCpu2.drainPermits();
        semPlanificador.drainPermits();
        semGeneradorInt.drainPermits();
        for (Semaphore semISR : semISRs) {
            semISR.drainPermits();
        }
        semISRs.clear();
    }
}
