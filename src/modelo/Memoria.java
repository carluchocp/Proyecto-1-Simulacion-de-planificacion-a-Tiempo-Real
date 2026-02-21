/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;
import estructuras.Cola;
import estructuras.Nodo;
import java.util.concurrent.Semaphore;

/**
 * 5.3 — Semáforo binario para exclusión mutua en TODAS las
 * operaciones sobre las colas compartidas y procesosEnRAM.
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
    private int procesosEnRAM;

    // 5.3 — Semáforo binario (mutex) para exclusión mutua
    private final Semaphore semaforo = new Semaphore(1, true);

    public Memoria(int capacidadMaxima) {
        this.colaNuevos = new Cola<>();
        this.colaListos = new Cola<>();
        this.colaBloqueados = new Cola<>();
        this.colaListosSuspendidos = new Cola<>();
        this.colaBloqueadosSuspendidos = new Cola<>();
        this.colaTerminados = new Cola<>();
        this.capacidadMaxima = capacidadMaxima;
        this.procesosEnRAM = 0;
    }

    // ======================== Cola de Nuevos ========================

    public void encolarNuevo(Proceso p) {
        semaforo.acquireUninterruptibly();
        try {
            colaNuevos.encolar(p);
        } finally {
            semaforo.release();
        }
    }

    public Proceso desencolarNuevo() {
        semaforo.acquireUninterruptibly();
        try {
            return colaNuevos.desencolar();
        } finally {
            semaforo.release();
        }
    }

    // ======================== Admitir proceso ========================

    public void admitirProceso(Proceso p) {
        semaforo.acquireUninterruptibly();
        try {
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
        } finally {
            semaforo.release();
        }
    }

    // ======================== Cola de Listos ========================

    public void encolarListo(Proceso p) {
        semaforo.acquireUninterruptibly();
        try {
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
        } finally {
            semaforo.release();
        }
    }

    public void reEncolarListo(Proceso p) {
        semaforo.acquireUninterruptibly();
        try {
            colaListos.encolar(p);
        } finally {
            semaforo.release();
        }
    }

    public Proceso desencolarListo() {
        semaforo.acquireUninterruptibly();
        try {
            return colaListos.desencolar();
        } finally {
            semaforo.release();
        }
    }

    // ======================== Cola de Bloqueados ========================

    public void encolarBloqueadoDirecto(Proceso p) {
        semaforo.acquireUninterruptibly();
        try {
            colaBloqueados.encolar(p);
        } finally {
            semaforo.release();
        }
    }

    public void moverBloqueadoAListo(Proceso p) {
        semaforo.acquireUninterruptibly();
        try {
            p.setEstado(EstadoProceso.LISTO);
            colaListos.encolar(p);
        } finally {
            semaforo.release();
        }
    }

    public Proceso desencolarBloqueado() {
        semaforo.acquireUninterruptibly();
        try {
            return colaBloqueados.desencolar();
        } finally {
            semaforo.release();
        }
    }

    // ======================== Cola de Terminados ========================

    public void encolarTerminado(Proceso p) {
        semaforo.acquireUninterruptibly();
        try {
            colaTerminados.encolar(p);
            procesosEnRAM--;
            if (procesosEnRAM < 0) procesosEnRAM = 0;
            System.out.println("[MEMORIA] " + p.getId() + " TERMINADO. En RAM: " + procesosEnRAM + "/" + capacidadMaxima);
        } finally {
            semaforo.release();
        }
    }

    // ======================== Suspensión inteligente ========================

    private Proceso encontrarMenosUrgenteEnRAM() {
        // NOTA: llamar solo dentro de bloque con semáforo ya adquirido
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
        // NOTA: llamar solo dentro de bloque con semáforo ya adquirido
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
        // NOTA: llamar solo dentro de bloque con semáforo ya adquirido
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
        semaforo.acquireUninterruptibly();
        try {
            if (colaBloqueados.estaVacia()) return;

            Proceso menosUrgente = null;
            Cola<Proceso> temp = new Cola<>();
            Proceso p;

            while ((p = colaBloqueados.desencolar()) != null) {
                if (menosUrgente == null || p.getTiempoRestanteDeadline() > menosUrgente.getTiempoRestanteDeadline()) {
                    if (menosUrgente != null) temp.encolar(menosUrgente);
                    menosUrgente = p;
                } else {
                    temp.encolar(p);
                }
            }

            while ((p = temp.desencolar()) != null) {
                colaBloqueados.encolar(p);
            }

            if (menosUrgente != null) {
                menosUrgente.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
                colaBloqueadosSuspendidos.encolar(menosUrgente);
                procesosEnRAM--;
                if (procesosEnRAM < 0) procesosEnRAM = 0;
                System.out.println("[MEMORIA] Suspendido: " + menosUrgente.getId()
                        + " Bloqueado -> Bloqueado/Suspendido. En RAM: " + procesosEnRAM + "/" + capacidadMaxima);
            }
        } finally {
            semaforo.release();
        }
    }

    public void intentarReactivar() {
        semaforo.acquireUninterruptibly();
        try {
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
        } finally {
            semaforo.release();
        }
    }

    // ======================== Configuración dinámica ========================

    public int getCapacidadMaxima() { return capacidadMaxima; }

    public void setCapacidadMaxima(int nuevaCapacidad) {
        semaforo.acquireUninterruptibly();
        try {
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
        } finally {
            semaforo.release();
        }
    }

    // ======================== Consultas ========================

    public boolean ramLlena() {
        semaforo.acquireUninterruptibly();
        try {
            return procesosEnRAM >= capacidadMaxima;
        } finally {
            semaforo.release();
        }
    }

    public int getProcesosEnRAM() {
        semaforo.acquireUninterruptibly();
        try {
            return procesosEnRAM;
        } finally {
            semaforo.release();
        }
    }

    // ======================== Getters de colas ========================

    public Cola<Proceso> getColaNuevos() { return colaNuevos; }
    public Cola<Proceso> getColaListos() { return colaListos; }
    public Cola<Proceso> getColaBloqueados() { return colaBloqueados; }
    public Cola<Proceso> getColaListosSuspendidos() { return colaListosSuspendidos; }
    public Cola<Proceso> getColaBloqueadosSuspendidos() { return colaBloqueadosSuspendidos; }
    public Cola<Proceso> getColaTerminados() { return colaTerminados; }
    public Semaphore getSemaforo() { return semaforo; }

    public void limpiar() {
        semaforo.acquireUninterruptibly();
        try {
            // Detener hilos de procesos en todas las colas antes de vaciar
            detenerHilosDeCola(colaNuevos);
            detenerHilosDeCola(colaListos);
            detenerHilosDeCola(colaBloqueados);
            detenerHilosDeCola(colaListosSuspendidos);
            detenerHilosDeCola(colaBloqueadosSuspendidos);
            // Los terminados ya tienen hilo detenido

            while (colaNuevos.desencolar() != null);
            while (colaListos.desencolar() != null);
            while (colaBloqueados.desencolar() != null);
            while (colaListosSuspendidos.desencolar() != null);
            while (colaBloqueadosSuspendidos.desencolar() != null);
            while (colaTerminados.desencolar() != null);
            procesosEnRAM = 0;
        } finally {
            semaforo.release();
        }
    }

    /**
     * 5.2 — Detener hilos de procesos en una cola antes de limpiar.
     */
    private void detenerHilosDeCola(Cola<Proceso> cola) {
        Nodo<Proceso> actual = cola.getPrimerNodo();
        while (actual != null) {
            actual.getContenido().detenerHilo();
            actual = actual.getSiguiente();
        }
    }
}
