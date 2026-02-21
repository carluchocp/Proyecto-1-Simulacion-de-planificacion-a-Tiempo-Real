/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;
import estructuras.Cola;
import estructuras.Nodo;
import java.util.concurrent.Semaphore;
/**
 *
 * @author carluchocp
 */
public class Memoria {

    private Cola<Proceso> colaNuevos;
    private Cola<Proceso> colaListos;
    private Cola<Proceso> colaBloqueados;
    private Cola<Proceso> colaListosSuspendidos;
    private Cola<Proceso> colaBloqueadosSuspendidos;
    private Cola<Proceso> colaTerminados;

    private int capacidadMaxima;
    private int procesosEnRAM;  // Listos + Bloqueados + En Ejecución

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

    // ======================== Admitir proceso (Nuevo → RAM o Suspendido) ========================

    public void admitirProceso(Proceso p) {
        try {
            semaforo.acquire();
            if (procesosEnRAM < capacidadMaxima) {
                p.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(p);
                procesosEnRAM++;
            } else {
                Proceso victima = encontrarMenosUrgenteEnRAM();
                if (victima != null && esMasUrgente(p, victima)) {
                    suspenderProcesoInterno(victima);
                    p.setEstado(EstadoProceso.LISTO);
                    colaListos.encolar(p);
                    procesosEnRAM++;
                } else {
                    p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
                    colaListosSuspendidos.encolar(p);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    // ======================== Cola de Listos ========================

    public void encolarListo(Proceso p) {
        try {
            semaforo.acquire();
            if (procesosEnRAM < capacidadMaxima) {
                if (p.getEstado() != EstadoProceso.LISTO) {
                    p.setEstado(EstadoProceso.LISTO);
                }
                colaListos.encolar(p);
                procesosEnRAM++;
            } else {
                p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
                colaListosSuspendidos.encolar(p);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    /**
     * Encola un proceso que YA está en RAM (ej: preemptado de CPU).
     * No modifica el contador de procesosEnRAM.
     */
    public void reEncolarListo(Proceso p) {
        try {
            semaforo.acquire();
            if (p.getEstado() != EstadoProceso.LISTO) {
                p.setEstado(EstadoProceso.LISTO);
            }
            colaListos.encolar(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    /**
     * Desencola un proceso listo para asignar a CPU.
     * El proceso sigue contando como "en RAM" (está en ejecución).
     */
    public Proceso desencolarListo() {
        Proceso p = null;
        try {
            semaforo.acquire();
            p = colaListos.desencolar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
        return p;
    }

    // ======================== Cola de Bloqueados ========================

    /**
     * Encola un proceso bloqueado. El proceso ya estaba en RAM (CPU),
     * así que no incrementa contador.
     */
    public void encolarBloqueadoDirecto(Proceso p) {
        try {
            semaforo.acquire();
            colaBloqueados.encolar(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    /**
     * Mueve un proceso de bloqueado a listo (E/S terminó).
     * No cambia contador porque sigue en RAM.
     */
    public void moverBloqueadoAListo(Proceso p) {
        try {
            semaforo.acquire();
            p.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(p);
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

    // ======================== Cola de Terminados ========================

    /**
     * Proceso terminó: sale de RAM.
     */
    public void encolarTerminado(Proceso p) {
        try {
            semaforo.acquire();
            colaTerminados.encolar(p);
            procesosEnRAM--;
            reactivarSuspendidoSiHayEspacio();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    // ======================== Suspensión inteligente ========================

    private Proceso encontrarMenosUrgenteEnRAM() {
        Proceso menosUrgente = null;
        int maxDeadline = -1;

        Nodo<Proceso> actual = colaListos.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            if (p.getTiempoRestanteDeadline() > maxDeadline) {
                maxDeadline = p.getTiempoRestanteDeadline();
                menosUrgente = p;
            }
            actual = actual.getSiguiente();
        }

        actual = colaBloqueados.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            if (p.getTiempoRestanteDeadline() > maxDeadline) {
                maxDeadline = p.getTiempoRestanteDeadline();
                menosUrgente = p;
            }
            actual = actual.getSiguiente();
        }
        return menosUrgente;
    }

    private boolean esMasUrgente(Proceso nuevo, Proceso existente) {
        if (nuevo.getTiempoRestanteDeadline() < existente.getTiempoRestanteDeadline()) {
            return true;
        }
        if (nuevo.getTiempoRestanteDeadline() == existente.getTiempoRestanteDeadline()) {
            return nuevo.getPrioridad() < existente.getPrioridad();
        }
        return false;
    }

    private void suspenderProcesoInterno(Proceso victima) {
        if (removerDeCola(colaListos, victima)) {
            victima.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
            colaListosSuspendidos.encolar(victima);
            procesosEnRAM--;
            System.out.println("[MEMORIA] Suspendido (Listo->ListoSusp): " + victima.getId());
            return;
        }
        if (removerDeCola(colaBloqueados, victima)) {
            victima.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
            colaBloqueadosSuspendidos.encolar(victima);
            procesosEnRAM--;
            System.out.println("[MEMORIA] Suspendido (Bloq->BloquSusp): " + victima.getId());
        }
    }

    private boolean removerDeCola(Cola<Proceso> cola, Proceso objetivo) {
        Cola<Proceso> temporal = new Cola<>();
        boolean encontrado = false;
        Proceso p;
        while ((p = cola.desencolar()) != null) {
            if (p.getId().equals(objetivo.getId()) && !encontrado) {
                encontrado = true;
            } else {
                temporal.encolar(p);
            }
        }
        while ((p = temporal.desencolar()) != null) {
            cola.encolar(p);
        }
        return encontrado;
    }

    private void reactivarSuspendidoSiHayEspacio() {
        while (procesosEnRAM < capacidadMaxima) {
            Proceso masUrgente = extraerMasUrgenteSuspendido();
            if (masUrgente == null) break;

            if (masUrgente.getEstado() == EstadoProceso.LISTO_SUSPENDIDO) {
                masUrgente.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(masUrgente);
                procesosEnRAM++;
                System.out.println("[MEMORIA] Reactivado (ListoSusp->Listo): " + masUrgente.getId());
            } else if (masUrgente.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
                masUrgente.setEstado(EstadoProceso.BLOQUEADO);
                colaBloqueados.encolar(masUrgente);
                procesosEnRAM++;
                System.out.println("[MEMORIA] Reactivado (BloquSusp->Bloq): " + masUrgente.getId());
            }
        }
    }

    private Proceso extraerMasUrgenteSuspendido() {
        Proceso candidatoListo = encontrarMasUrgenteEnCola(colaListosSuspendidos);
        Proceso candidatoBloq = encontrarMasUrgenteEnCola(colaBloqueadosSuspendidos);

        if (candidatoListo == null && candidatoBloq == null) return null;

        Proceso elegido;
        Cola<Proceso> colaOrigen;

        if (candidatoListo == null) {
            elegido = candidatoBloq;
            colaOrigen = colaBloqueadosSuspendidos;
        } else if (candidatoBloq == null) {
            elegido = candidatoListo;
            colaOrigen = colaListosSuspendidos;
        } else if (esMasUrgente(candidatoListo, candidatoBloq)) {
            elegido = candidatoListo;
            colaOrigen = colaListosSuspendidos;
        } else {
            elegido = candidatoBloq;
            colaOrigen = colaBloqueadosSuspendidos;
        }

        removerDeCola(colaOrigen, elegido);
        return elegido;
    }

    private Proceso encontrarMasUrgenteEnCola(Cola<Proceso> cola) {
        Proceso masUrgente = null;
        int minDeadline = Integer.MAX_VALUE;
        Nodo<Proceso> actual = cola.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            if (p.getTiempoRestanteDeadline() < minDeadline) {
                minDeadline = p.getTiempoRestanteDeadline();
                masUrgente = p;
            }
            actual = actual.getSiguiente();
        }
        return masUrgente;
    }

    // ======================== Métodos públicos de suspensión ========================

    public void suspenderMenosUrgente() {
        try {
            semaforo.acquire();
            Proceso victima = encontrarMenosUrgenteEnRAM();
            if (victima != null) {
                suspenderProcesoInterno(victima);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    public void intentarReactivar() {
        try {
            semaforo.acquire();
            reactivarSuspendidoSiHayEspacio();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    // ======================== Configuración dinámica ========================

    public int getCapacidadMaxima() { return capacidadMaxima; }

    public void setCapacidadMaxima(int nuevaCapacidad) {
        try {
            semaforo.acquire();
            this.capacidadMaxima = nuevaCapacidad;
            while (procesosEnRAM > capacidadMaxima) {
                Proceso victima = encontrarMenosUrgenteEnRAM();
                if (victima != null) {
                    suspenderProcesoInterno(victima);
                } else {
                    break;
                }
            }
            reactivarSuspendidoSiHayEspacio();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    // ======================== Consultas ========================

    public boolean ramLlena() { return procesosEnRAM >= capacidadMaxima; }
    public int getProcesosEnRAM() { return procesosEnRAM; }

    // ======================== Getters de colas ========================

    public Cola<Proceso> getColaNuevos() { return colaNuevos; }
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaListosSuspendidos() { return colaListosSuspendidos; }
    public Cola<Proceso> getColaBloqueadosSuspendidos() { return colaBloqueadosSuspendidos; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }
    public Semaphore getSemaforo() { return semaforo; }
}
