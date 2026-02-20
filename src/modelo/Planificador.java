/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author diego
 */
public class Planificador extends Thread {
    
    private Memoria memoria;
    private Reloj reloj;
    private boolean enEjecucion;

    public Planificador(Memoria memoria, Reloj reloj) {
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
                gestionarBloqueados();
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                enEjecucion = false;
            }
        }
    }

    private void gestionarBloqueados() {
        Proceso p = memoria.desencolarBloqueado();
        if (p != null) {
            p.setEstado(EstadoProceso.LISTO);
            memoria.encolarListo(p);
        }
    }

    public void detener() {
        this.enEjecucion = false;
    }
}
