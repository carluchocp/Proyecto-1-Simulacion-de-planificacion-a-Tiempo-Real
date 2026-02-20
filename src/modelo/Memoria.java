/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;
import estructuras.Cola;
import java.util.concurrent.Semaphore;
/**
 *
 * @author diego
 */
public class Memoria {
    
    // Colas del sistema (usando tu estructura personalizada)
    private Cola<Proceso> colaListos;
    private Cola<Proceso> colaBloqueados;
    private Cola<Proceso> colaListosSuspendidos;
    private Cola<Proceso> colaBloqueadosSuspendidos;
    private Cola<Proceso> colaTerminados;

    // Semáforos para garantizar Exclusión Mutua
    private Semaphore mutexListos;
    private Semaphore mutexBloqueados;
    private Semaphore mutexSuspendidos; 
    private Semaphore mutexTerminados;
    
    // Parámetro de capacidad de la RAM (para luego manejar el Swap)
    private int capacidadMaximaRAM;
    private int procesosEnRAM;

    public Memoria(int capacidadMaximaRAM) {
        // Inicializamos las colas
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaListosSuspendidos = new Cola<>();
        this.colaBloqueadosSuspendidos = new Cola<>();
        this.colaTerminados = new Cola<>();

        // Inicializamos los semáforos en 1 (actúan como candados mutex)
        this.mutexListos = new Semaphore(1);
        this.mutexBloqueados = new Semaphore(1);
        this.mutexSuspendidos = new Semaphore(1);
        this.mutexTerminados = new Semaphore(1);
        
        this.capacidadMaximaRAM = capacidadMaximaRAM;
        this.procesosEnRAM = 0;
    }

    // --- MÉTODOS PARA LA COLA DE LISTOS (Protegidos por Semáforo) ---

    public void encolarListo(Proceso p) {
        try {
            mutexListos.acquire(); // Cierra el candado
            colaListos.encolar(p);
            procesosEnRAM++;
        } catch (InterruptedException e) {
            System.out.println("Interrupción al encolar en Listos: " + e.getMessage());
        } finally {
            mutexListos.release(); // Abre el candado SIEMPRE al terminar
        }
    }

    public Proceso desencolarListo() {
        Proceso p = null;
        try {
            mutexListos.acquire();
            if (!colaListos.esVacia()) {
                p = colaListos.desencolar();
                procesosEnRAM--;
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupción al desencolar de Listos: " + e.getMessage());
        } finally {
            mutexListos.release();
        }
        return p;
    }

    // --- MÉTODOS PARA LA COLA DE BLOQUEADOS (Protegidos por Semáforo) ---

    public void encolarBloqueado(Proceso p) {
        try {
            mutexBloqueados.acquire();
            colaBloqueados.encolar(p);
        } catch (InterruptedException e) {
            System.out.println("Interrupción al encolar en Bloqueados: " + e.getMessage());
        } finally {
            mutexBloqueados.release();
        }
    }

    public Proceso desencolarBloqueado() {
        Proceso p = null;
        try {
            mutexBloqueados.acquire();
            if (!colaBloqueados.esVacia()) {
                p = colaBloqueados.desencolar();
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupción al desencolar de Bloqueados: " + e.getMessage());
        } finally {
            mutexBloqueados.release();
        }
        return p;
    }
    
    // --- GETTERS ---
    
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaListosSuspendidos() { return colaListosSuspendidos; }
    public Cola<Proceso> getColaBloqueadosSuspendidos() { return colaBloqueadosSuspendidos; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }

    public int getCapacidadMaximaRAM() { return capacidadMaximaRAM; }
    public int getProcesosEnRAM() { return procesosEnRAM; }
}
