/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import java.util.Random;

/**
 * 6.2 — Genera eventos asíncronos aleatorios.
 *
 * @author carluchocp
 */
public class GeneradorInterrupciones extends Thread {

    private final CPU cpu1;
    private final CPU cpu2;
    private final Reloj reloj;
    private final Planificador planificador;
    private final Random random;
    private volatile boolean enEjecucion;
    private volatile boolean pausado;
    private int contadorInterrupciones;
    private int intervaloMinimo;
    private int intervaloMaximo;
    private InterrupcionListener listener;

    public GeneradorInterrupciones(CPU cpu1, CPU cpu2, Reloj reloj, Planificador planificador) {
        this.cpu1 = cpu1;
        this.cpu2 = cpu2;
        this.reloj = reloj;
        this.planificador = planificador;
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

        while (enEjecucion) {
            if (!pausado) {
                int ciclosEspera = intervaloMinimo
                        + random.nextInt(intervaloMaximo - intervaloMinimo + 1);
                int cicloObjetivo = reloj.getCicloGlobal() + ciclosEspera;

                while (reloj.getCicloGlobal() < cicloObjetivo && enEjecucion && !pausado) {
                    try { Thread.sleep(50); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }

                if (!pausado && enEjecucion) {
                    generarInterrupcion();
                }
            } else {
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
                contadorInterrupciones, tipo, cpuObjetivo, reloj, planificador);
        interrupcion.setListener(listener);
        interrupcion.start();
    }

    public void pausar() { this.pausado = true; }
    public void reanudar() { this.pausado = false; }
    public boolean isPausado() { return pausado; }
    public void detener() { this.enEjecucion = false; }
    public int getContadorInterrupciones() { return contadorInterrupciones; }
}
