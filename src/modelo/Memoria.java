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
    private int capacidadMaxima;
    private int procesosEnRAM;

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

    // ======================== Admitir proceso (Nuevo → Listo o Suspendido) ========================

    /**
     * Intenta admitir un proceso en RAM. Si hay espacio, va a Listo.
     * Si no hay espacio, intenta suspender al proceso con deadline más lejano
     * para hacer lugar al nuevo (si el nuevo es más urgente).
     * Si el nuevo no es más urgente, va directo a Listo/Suspendido.
     */
    public void admitirProceso(Proceso p) {
        try {
            semaforo.acquire();
            if (procesosEnRAM < capacidadMaxima) {
                p.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(p);
                procesosEnRAM++;
            } else {
                // RAM llena: ver si podemos desalojar a alguien menos urgente
                Proceso victima = encontrarMenosUrgenteEnRAM();
                if (victima != null && esmasUrgente(p, victima)) {
                    // Suspender la víctima y meter al nuevo
                    suspenderProcesoInterno(victima);
                    p.setEstado(EstadoProceso.LISTO);
                    colaListos.encolar(p);
                    procesosEnRAM++;
                } else {
                    // El nuevo no es más urgente, va a suspendido
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
                p.setEstado(EstadoProceso.LISTO);
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

    public Proceso desencolarListo() {
        Proceso p = null;
        try {
            semaforo.acquire();
            p = colaListos.desencolar();
            if (p != null) {
                procesosEnRAM--;
                reactivarSuspendidoSiHayEspacio();
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
            // Ya está en RAM, no incrementar contador
            colaBloqueados.encolar(p);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    /**
     * Mueve un proceso bloqueado a listo (cuando termina su E/S).
     * Si no hay espacio en RAM (fue desalojado otro), intenta reactivar.
     */
    public void desbloquearProceso(Proceso p) {
        try {
            semaforo.acquire();
            p.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(p);
            // El proceso ya estaba contado en RAM
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

    // ======================== Suspensión inteligente ========================

    /**
     * Busca el proceso con deadline más lejano entre Listos y Bloqueados.
     * Este es el candidato a ser suspendido cuando la RAM está llena.
     */
    private Proceso encontrarMenosUrgenteEnRAM() {
        Proceso menosUrgente = null;
        int maxDeadline = -1;

        // Buscar en cola de Listos
        Nodo<Proceso> actual = colaListos.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            if (p.getTiempoRestanteDeadline() > maxDeadline) {
                maxDeadline = p.getTiempoRestanteDeadline();
                menosUrgente = p;
            }
            actual = actual.getSiguiente();
        }

        // Buscar en cola de Bloqueados
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

    /**
     * Compara urgencia: menor tiempoRestanteDeadline = más urgente.
     * A igual deadline, mayor prioridad (número menor) gana.
     */
    private boolean esmasUrgente(Proceso nuevo, Proceso existente) {
        if (nuevo.getTiempoRestanteDeadline() < existente.getTiempoRestanteDeadline()) {
            return true;
        }
        if (nuevo.getTiempoRestanteDeadline() == existente.getTiempoRestanteDeadline()) {
            return nuevo.getPrioridad() < existente.getPrioridad();
        }
        return false;
    }

    /**
     * Suspende un proceso específico (lo saca de su cola y lo mueve a suspendido).
     * DEBE llamarse con semáforo ya adquirido.
     */
    private void suspenderProcesoInterno(Proceso victima) {
        // Intentar remover de Listos
        if (removerDeCola(colaListos, victima)) {
            victima.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
            colaListosSuspendidos.encolar(victima);
            procesosEnRAM--;
            System.out.println("[MEMORIA] Suspendido (Listo→ListoSusp): " + victima.getId()
                    + " | Deadline restante: " + victima.getTiempoRestanteDeadline());
            return;
        }

        // Intentar remover de Bloqueados
        if (removerDeCola(colaBloqueados, victima)) {
            victima.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
            colaBloqueadosSuspendidos.encolar(victima);
            procesosEnRAM--;
            System.out.println("[MEMORIA] Suspendido (Bloq→BloquSusp): " + victima.getId()
                    + " | Deadline restante: " + victima.getTiempoRestanteDeadline());
        }
    }

    /**
     * Remueve un proceso específico de una cola.
     * Reconstruye la cola sin ese proceso.
     */
    private boolean removerDeCola(Cola<Proceso> cola, Proceso objetivo) {
        Cola<Proceso> temporal = new Cola<>();
        boolean encontrado = false;

        Proceso p;
        while ((p = cola.desencolar()) != null) {
            if (p.getId().equals(objetivo.getId()) && !encontrado) {
                encontrado = true; // no lo re-encolamos
            } else {
                temporal.encolar(p);
            }
        }

        // Re-encolar los que quedaron
        while ((p = temporal.desencolar()) != null) {
            cola.encolar(p);
        }

        return encontrado;
    }

    /**
     * Si hay espacio en RAM, trae el proceso suspendido con deadline más cercano.
     * DEBE llamarse con semáforo ya adquirido.
     */
    private void reactivarSuspendidoSiHayEspacio() {
        while (procesosEnRAM < capacidadMaxima) {
            Proceso masUrgente = extraerMasUrgenteSuspendido();
            if (masUrgente == null) {
                break;
            }

            if (masUrgente.getEstado() == EstadoProceso.LISTO_SUSPENDIDO) {
                masUrgente.setEstado(EstadoProceso.LISTO);
                colaListos.encolar(masUrgente);
                procesosEnRAM++;
                System.out.println("[MEMORIA] Reactivado (ListoSusp→Listo): " + masUrgente.getId());
            } else if (masUrgente.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO) {
                masUrgente.setEstado(EstadoProceso.BLOQUEADO);
                colaBloqueados.encolar(masUrgente);
                procesosEnRAM++;
                System.out.println("[MEMORIA] Reactivado (BloquSusp→Bloq): " + masUrgente.getId());
            }
        }
    }

    /**
     * Encuentra y extrae el proceso con menor tiempoRestanteDeadline
     * entre ambas colas de suspendidos.
     */
    private Proceso extraerMasUrgenteSuspendido() {
        Proceso candidatoListo = encontrarMasUrgenteEnCola(colaListosSuspendidos);
        Proceso candidatoBloq = encontrarMasUrgenteEnCola(colaBloqueadosSuspendidos);

        if (candidatoListo == null && candidatoBloq == null) {
            return null;
        }

        Proceso elegido;
        Cola<Proceso> colaOrigen;

        if (candidatoListo == null) {
            elegido = candidatoBloq;
            colaOrigen = colaBloqueadosSuspendidos;
        } else if (candidatoBloq == null) {
            elegido = candidatoListo;
            colaOrigen = colaListosSuspendidos;
        } else if (esmasUrgente(candidatoListo, candidatoBloq)) {
            elegido = candidatoListo;
            colaOrigen = colaListosSuspendidos;
        } else {
            elegido = candidatoBloq;
            colaOrigen = colaBloqueadosSuspendidos;
        }

        removerDeCola(colaOrigen, elegido);
        return elegido;
    }

    /**
     * Encuentra el proceso con menor tiempoRestanteDeadline en una cola (sin removerlo).
     */
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

    /**
     * Fuerza la suspensión del proceso menos urgente en RAM.
     * Útil para el planificador de mediano plazo.
     */
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

    /**
     * Intenta reactivar procesos suspendidos si hay espacio.
     * Llamar periódicamente desde el planificador.
     */
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

    // ======================== Cola de Terminados ========================

    public void encolarTerminado(Proceso p) {
        try {
            semaforo.acquire();
            colaTerminados.encolar(p);
            // No está en RAM, no decrementar (ya se decrementó al desencolar de Listos)
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
            // Si se redujo la capacidad, suspender excedentes
            while (procesosEnRAM > capacidadMaxima) {
                Proceso victima = encontrarMenosUrgenteEnRAM();
                if (victima != null) {
                    suspenderProcesoInterno(victima);
                } else {
                    break;
                }
            }
            // Si se aumentó, reactivar suspendidos
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

    public void incrementarProcesosEnRAM() {
        try {
            semaforo.acquire();
            procesosEnRAM++;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }

    public void decrementarProcesosEnRAM() {
        try {
            semaforo.acquire();
            procesosEnRAM--;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
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
