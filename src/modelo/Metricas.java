package modelo;

import estructuras.Cola;
import estructuras.Nodo;

/**
 * 9.x — Centraliza las métricas de rendimiento del simulador.
 */
public class Metricas {

    // Historial de utilización de CPU (porcentaje por ciclo)
    // Usamos arreglos simples para no usar ArrayList
    private double[] historialUtilizacion;
    private int[] historialCiclos;
    private int tamanoHistorial;
    private int capacidad;

    // Contadores de deadline
    private int deadlineCumplidos;
    private int deadlineFallidos;

    // Para throughput
    private int procesosCompletados;

    public Metricas() {
        this.capacidad = 2000;
        this.historialUtilizacion = new double[capacidad];
        this.historialCiclos = new int[capacidad];
        this.tamanoHistorial = 0;
        this.deadlineCumplidos = 0;
        this.deadlineFallidos = 0;
        this.procesosCompletados = 0;
    }

    /**
     * 9.1 — Registra la utilización del procesador en un ciclo dado.
     * @param ciclo número de ciclo global
     * @param cpu1Activa true si CPU1 tiene proceso
     * @param cpu2Activa true si CPU2 tiene proceso
     */
    public synchronized void registrarUtilizacion(int ciclo, boolean cpu1Activa, boolean cpu2Activa) {
        int cpusActivas = (cpu1Activa ? 1 : 0) + (cpu2Activa ? 1 : 0);
        double utilizacion = (cpusActivas / 2.0) * 100.0;

        if (tamanoHistorial >= capacidad) {
            expandirArreglos();
        }

        historialCiclos[tamanoHistorial] = ciclo;
        historialUtilizacion[tamanoHistorial] = utilizacion;
        tamanoHistorial++;
    }

    private void expandirArreglos() {
        int nuevaCapacidad = capacidad * 2;
        double[] nuevoUtil = new double[nuevaCapacidad];
        int[] nuevoCiclos = new int[nuevaCapacidad];
        System.arraycopy(historialUtilizacion, 0, nuevoUtil, 0, capacidad);
        System.arraycopy(historialCiclos, 0, nuevoCiclos, 0, capacidad);
        historialUtilizacion = nuevoUtil;
        historialCiclos = nuevoCiclos;
        capacidad = nuevaCapacidad;
    }

    public synchronized int getTamanoHistorial() { return tamanoHistorial; }
    public synchronized int getCicloEn(int index) { return historialCiclos[index]; }
    public synchronized double getUtilizacionEn(int index) { return historialUtilizacion[index]; }

    /**
     * 9.1 — Utilización promedio total.
     */
    public synchronized double getUtilizacionPromedio() {
        if (tamanoHistorial == 0) return 0;
        double suma = 0;
        for (int i = 0; i < tamanoHistorial; i++) {
            suma += historialUtilizacion[i];
        }
        return suma / tamanoHistorial;
    }

    /**
     * 9.2 — Tasa de éxito: % de procesos terminados antes de su deadline.
     */
    public synchronized double getTasaExito() {
        int total = deadlineCumplidos + deadlineFallidos;
        if (total == 0) return 0;
        return (deadlineCumplidos * 100.0) / total;
    }

    /**
     * 9.5 — Registra que un proceso cumplió o falló su deadline al terminar.
     */
    public synchronized void registrarFinProceso(Proceso p) {
        procesosCompletados++;
        if (p.getTiempoRestanteDeadline() >= 0) {
            deadlineCumplidos++;
        } else {
            deadlineFallidos++;
        }
    }

    public synchronized int getDeadlineCumplidos() { return deadlineCumplidos; }
    public synchronized int getDeadlineFallidos() { return deadlineFallidos; }
    public synchronized int getProcesosCompletados() { return procesosCompletados; }

    /**
     * 9.3 — Throughput: procesos completados / ciclos transcurridos.
     */
    public synchronized double getThroughput(int cicloActual) {
        if (cicloActual == 0) return 0;
        return (double) procesosCompletados / cicloActual;
    }

    /**
     * 9.4 — Tiempo de espera promedio calculado desde la cola de terminados.
     */
    public synchronized double getTiempoEsperaPromedio(Cola<Proceso> colaTerminados) {
        if (colaTerminados.estaVacia()) return 0;
        int suma = 0;
        int count = 0;
        Nodo<Proceso> actual = colaTerminados.getPrimerNodo();
        while (actual != null) {
            suma += actual.getContenido().getTiempoEspera();
            count++;
            actual = actual.getSiguiente();
        }
        return count > 0 ? (double) suma / count : 0;
    }

    /**
     * Reinicia todas las métricas.
     */
    public synchronized void reiniciar() {
        tamanoHistorial = 0;
        deadlineCumplidos = 0;
        deadlineFallidos = 0;
        procesosCompletados = 0;
    }
}
