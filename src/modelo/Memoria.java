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
        colaNuevos.encolar(p);
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
        if (procesosEnRAM < capacidadMaxima) {
            p.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(p);
            procesosEnRAM++;
            System.out.println("[MEMORIA] " + p.getId() + " admitido -> Listo. En RAM: " + procesosEnRAM + "/" + capacidadMaxima);
        } else {
            p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
            colaListosSuspendidos.encolar(p);
            System.out.println("[MEMORIA] " + p.getId() + " admitido -> Listo/Suspendido (RAM llena). En RAM: " + procesosEnRAM + "/" + capacidadMaxima);
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
        colaListos.encolar(p);
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
        colaBloqueados.encolar(p);
    }

    /**
     * Mueve un proceso de bloqueado a listo (E/S terminó).
     * No cambia contador porque sigue en RAM.
     */
    public void moverBloqueadoAListo(Proceso p) {
        p.setEstado(EstadoProceso.LISTO);
        colaListos.encolar(p);
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
     * Cuando un proceso TERMINA, se saca de RAM y se encola en terminados.
     * DEBE decrementar procesosEnRAM para liberar espacio.
     */
    public synchronized void encolarTerminado(Proceso p) {
        colaTerminados.encolar(p);
        procesosEnRAM--;
        if (procesosEnRAM < 0) procesosEnRAM = 0;
        System.out.println("[MEMORIA] " + p.getId() + " TERMINADO. En RAM: " + procesosEnRAM + "/" + capacidadMaxima);
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
        if (colaBloqueados.estaVacia()) return;

        // Encontrar el menos urgente (mayor tiempoRestanteDeadline)
        Proceso menosUrgente = null;
        estructuras.Cola<Proceso> temp = new estructuras.Cola<>();
        Proceso p;

        while ((p = colaBloqueados.desencolar()) != null) {
            if (menosUrgente == null || p.getTiempoRestanteDeadline() > menosUrgente.getTiempoRestanteDeadline()) {
                if (menosUrgente != null) temp.encolar(menosUrgente);
                menosUrgente = p;
            } else {
                temp.encolar(p);
            }
        }

        // Re-encolar los que no se suspendieron
        while ((p = temp.desencolar()) != null) {
            colaBloqueados.encolar(p);
        }

        // Suspender el menos urgente
        if (menosUrgente != null) {
            menosUrgente.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
            colaBloqueadosSuspendidos.encolar(menosUrgente);
            procesosEnRAM--;
            if (procesosEnRAM < 0) procesosEnRAM = 0;
            System.out.println("[MEMORIA] Suspendido: " + menosUrgente.getId()
                    + " Bloqueado -> Bloqueado/Suspendido. En RAM: " + procesosEnRAM + "/" + capacidadMaxima);
        }
    }

    /**
     * Mueve procesos de Listo/Suspendido a Listo mientras haya espacio en RAM.
     * Mueve TODOS los que quepan, no solo 1.
     */
    public synchronized void intentarReactivar() {
        while (!colaListosSuspendidos.estaVacia() && procesosEnRAM < capacidadMaxima) {
            Proceso p = colaListosSuspendidos.desencolar();
            if (p != null) {
                p.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(p);
                procesosEnRAM++;
                System.out.println("[MEMORIA] Reactivado: " + p.getId()
                        + " Listo/Suspendido -> Listo. En RAM: " + procesosEnRAM + "/" + capacidadMaxima);
            }
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

    public void limpiar() {
        try {
            semaforo.acquire();
            // Vaciar todas las colas
            while (colaNuevos.desencolar() != null);
            while (colaListos.desencolar() != null);
            while (colaBloqueados.desencolar() != null);
            while (colaListosSuspendidos.desencolar() != null);
            while (colaBloqueadosSuspendidos.desencolar() != null);
            while (colaTerminados.desencolar() != null);
            procesosEnRAM = 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }
}
