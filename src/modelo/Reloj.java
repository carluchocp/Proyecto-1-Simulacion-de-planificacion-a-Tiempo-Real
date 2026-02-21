/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author carluchocp
 */
public class Reloj extends Thread {

    private volatile int cicloGlobal;
    private volatile int duracionCiclo; // en ms
    private volatile boolean enEjecucion;
    private volatile boolean pausado;

    public Reloj() {
        this.cicloGlobal = 0;
        this.duracionCiclo = 500;
        this.enEjecucion = false;
        this.pausado = true; // Empieza pausado
    }

    @Override
    public void run() {
        this.enEjecucion = true;
        while (enEjecucion) {
            try {
                Thread.sleep(duracionCiclo);
                if (!pausado) {
                    cicloGlobal++;
                }
            } catch (InterruptedException e) {
                enEjecucion = false;
            }
        }
    }

    public int getCicloGlobal() { return cicloGlobal; }
    public int getDuracionCiclo() { return duracionCiclo; }
    public void setDuracionCiclo(int duracionCiclo) { this.duracionCiclo = duracionCiclo; }

    public void pausar() { this.pausado = true; }
    public void reanudar() { this.pausado = false; }
    public boolean isPausado() { return pausado; }

    public void detener() { this.enEjecucion = false; }
}
