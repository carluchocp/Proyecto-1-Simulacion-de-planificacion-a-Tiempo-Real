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

    // --- Colas de estado ---
    private Cola<Proceso> colaNuevos;
    private Cola<Proceso> colaListos;
    private Cola<Proceso> colaBloqueados;
    private Cola<Proceso> colaListosSuspendidos;
    private Cola<Proceso> colaBloqueadosSuspendidos;
    private Cola<Proceso> colaTerminados;

    // --- Capacidad de memoria principal ---
    private int capacidadMaxima;   // máximo de procesos en RAM (Listos + Bloqueados + Ejecución)
    private int procesosEnRAM;     // contador actual

    // --- Semáforo para exclusión mutua ---
    private Semaphore semaforo;

    public Memoria(int capacidadMaxima) {
        this.colaNuevos = new Cola<>();
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaListosSuspendidos = new Cola<>();
        this.colaBloqueadosSuspendidos = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.capacidadMaxima = capacidadMaxima;
        this.procesosEnRAM = 0;
        this.semaforo = new Semaphore(1);
    }

    // ======================== Cola de Nuevos ========================

    public void encolarNuevo(Proceso p) {
        try {
            semaforo.acquire();
            colaNuevos.encolar(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    public Proceso desencolarNuevo() {
        Proceso p = null;
        try {
            semaforo.acquire();
            p = colaNuevos.desencolar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
        return p;
    }

    // ======================== Cola de Listos ========================

    public void encolarListo(Proceso p) {
        try {
            semaforo.acquire();
            if (procesosEnRAM < capacidadMaxima) {
                p.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(p);
                procesosEnRAM++;
            } else {
                // RAM llena: enviar a Listo/Suspendido
                p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
                colaListosSuspendidos.encolar(p);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    public Proceso desencolarListo() {
        Proceso p = null;
        try {
            semaforo.acquire();
            p = colaListos.desencolar();
            if (p != null) {
                procesosEnRAM--;
            }
            // Intentar traer uno de Listo/Suspendido si hay espacio
            if (p != null && procesosEnRAM < capacidadMaxima) {
                Proceso suspendido = colaListosSuspendidos.desencolar();
                if (suspendido != null) {
                    suspendido.setEstado(EstadoProceso.LISTO);
                    colaListos.encolar(suspendido);
                    procesosEnRAM++;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
        return p;
    }

    // ======================== Cola de Bloqueados ========================

    public void encolarBloqueado(Proceso p) {
        try {
            semaforo.acquire();
            p.setEstado(EstadoProceso.BLOQUEADO);
            colaBloqueados.encolar(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    public Proceso desencolarBloqueado() {
        Proceso p = null;
        try {
            semaforo.acquire();
            p = colaBloqueados.desencolar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
        return p;
    }

    // ======================== Cola de Listo/Suspendido ========================

    public void encolarListoSuspendido(Proceso p) {
        try {
            semaforo.acquire();
            p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
            colaListosSuspendidos.encolar(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    public Proceso desencolarListoSuspendido() {
        Proceso p = null;
        try {
            semaforo.acquire();
            p = colaListosSuspendidos.desencolar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
        return p;
    }

    // ======================== Cola de Bloqueado/Suspendido ========================

    public void encolarBloqueadoSuspendido(Proceso p) {
        try {
            semaforo.acquire();
            p.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
            colaBloqueadosSuspendidos.encolar(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    public Proceso desencolarBloqueadoSuspendido() {
        Proceso p = null;
        try {
            semaforo.acquire();
            p = colaBloqueadosSuspendidos.desencolar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
        return p;
    }

    // ======================== Cola de Terminados ========================

    public void encolarTerminado(Proceso p) {
        try {
            semaforo.acquire();
            colaTerminados.encolar(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    // ======================== Suspensión por saturación ========================

    /**
     * Suspende un proceso bloqueado a Bloqueado/Suspendido para liberar RAM.
     */
    public void suspenderBloqueado() {
        try {
            semaforo.acquire();
            Proceso p = colaBloqueados.desencolar();
            if (p != null) {
                p.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
                colaBloqueadosSuspendidos.encolar(p);
                procesosEnRAM--;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    /**
     * Reactiva un proceso de Bloqueado/Suspendido a Listo/Suspendido
     * (cuando su E/S terminó pero aún no hay espacio en RAM).
     */
    public void reactivarBloqueadoSuspendido() {
        try {
            semaforo.acquire();
            Proceso p = colaBloqueadosSuspendidos.desencolar();
            if (p != null) {
                p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
                colaListosSuspendidos.encolar(p);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    // ======================== Consultas ========================

    public boolean ramLlena() {
        return procesosEnRAM >= capacidadMaxima;
    }

    public int getProcesosEnRAM() {
        return procesosEnRAM;
    }

    public void incrementarProcesosEnRAM() {
        procesosEnRAM++;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public void setCapacidadMaxima(int capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    // ======================== Getters de colas (para UI) ========================

    public Cola<Proceso> getColaNuevos() { return colaNuevos; }
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaListosSuspendidos() { return colaListosSuspendidos; }
    public Cola<Proceso> getColaBloqueadosSuspendidos() { return colaBloqueadosSuspendidos; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }

    public Semaphore getSemaforo() { return semaforo; }
}
