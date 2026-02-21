/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author diego
 */
public class Reloj extends Thread {
    
    private int cicloGlobal;
    private int duracionCiclo; 
    private boolean enEjecucion;

    public Reloj() {
        this.cicloGlobal = 0;
        
        this.duracionCiclo = 1000; 
        this.enEjecucion = false;
    }

    
    @Override
    public void run() {
        this.enEjecucion = true;
        
        while (enEjecucion) {
            try {
                
                Thread.sleep(duracionCiclo);
                
                
                cicloGlobal++;
                
               
                
            } catch (InterruptedException e) {
                System.out.println("El reloj del sistema fue interrumpido.");
            }
        }
    }

    

    public int getCicloGlobal() {
        return cicloGlobal;
    }

    public int getDuracionCiclo() {
        return duracionCiclo;
    }

    public void setDuracionCiclo(int duracionCiclo) {
        this.duracionCiclo = duracionCiclo;
    }


    public void detener() {
        this.enEjecucion = false;
    }
}
