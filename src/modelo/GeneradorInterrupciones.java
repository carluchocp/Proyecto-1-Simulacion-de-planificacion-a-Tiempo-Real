/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * 6.2 — Genera eventos asíncronos aleatorios, sincronizado con el reloj.
 *
 * @author carluchocp
 */
public class GeneradorInterrupciones extends Thread {

    private final CPU cpu1;
    private final CPU cpu2;
    private final Reloj reloj;
    private final Planificador planificador;
    private final Memoria memoria;
    private final Random random;
    private volatile boolean enEjecucion;
    private volatile boolean pausado;
    private int contadorInterrupciones;
    private int intervaloMinimo;
    private int intervaloMaximo;
    private InterrupcionListener listener;

    public GeneradorInterrupciones(CPU cpu1, CPU cpu2, Reloj reloj,
                                   Planificador planificador, Memoria memoria) {
        this.cpu1 = cpu1;
        this.cpu2 = cpu2;
        this.reloj = reloj;
        this.planificador = planificador;
        this.memoria = memoria;
        this.random = new Random();
        this.enEjecucion = false;
        this.pausado = true;
        this.contadorInterrupciones = 0;
        this.intervaloMinimo = 10;
        this.intervaloMaximo = 30;
        this.setName("GeneradorInterrupciones");
        this.setDaemon(true);
    }

    public void setListener(InterrupcionListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        this.enEjecucion = true;
        Semaphore semReloj = reloj.getSemGeneradorInt();

        while (enEjecucion) {
            if (!pausado) {
                // Determinar cuántos ticks esperar antes de la próxima interrupción
                int ciclosEspera = intervaloMinimo
                        + random.nextInt(intervaloMaximo - intervaloMinimo + 1);

                // Esperar la cantidad de ticks usando el semáforo del reloj
                for (int i = 0; i < ciclosEspera && enEjecucion && !pausado; i++) {
                    try {
                        semReloj.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                if (!pausado && enEjecucion) {
                    generarInterrupcion();
                }
            } else {
                // Cuando está pausado, drenar permits acumulados y esperar
                semReloj.drainPermits();
                try { Thread.sleep(100); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private void generarInterrupcion() {
        TipoInterrupcion[] tipos = TipoInterrupcion.values();
        TipoInterrupcion tipo = tipos[random.nextInt(tipos.length)];

        CPU cpuObjetivo = random.nextBoolean() ? cpu1 : cpu2;
        if (cpuObjetivo.isEnInterrupcion()) {
            cpuObjetivo = (cpuObjetivo == cpu1) ? cpu2 : cpu1;
            if (cpuObjetivo.isEnInterrupcion()) {
                return;
            }
        }

        contadorInterrupciones++;

        Interrupcion interrupcion = new Interrupcion(
                contadorInterrupciones, tipo, cpuObjetivo, reloj, planificador, memoria);
        interrupcion.setListener(listener);
        interrupcion.start();
    }

    public void pausar() { this.pausado = true; }
    public void reanudar() { this.pausado = false; }
    public boolean isPausado() { return pausado; }
    public void detener() { this.enEjecucion = false; }
    public int getContadorInterrupciones() { return contadorInterrupciones; }

    public void setIntervaloMinimo(int min) { this.intervaloMinimo = min; }
    public void setIntervaloMaximo(int max) { this.intervaloMaximo = max; }
}
