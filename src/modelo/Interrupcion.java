/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import java.util.concurrent.Semaphore;

/**
 * Cada interrupción corre en un Thread independiente (6.3).
 * Suspende el proceso actual en la CPU objetivo (6.4),
 * ejecuta la ISR sincronizada con el reloj (6.5), re-planifica (6.6),
 * y restaura el proceso en la misma CPU (6.7).
 *
 * @author carluchocp
 */
public class Interrupcion extends Thread {

    private final int idInterrupcion;
    private final TipoInterrupcion tipo;
    private final CPU cpuObjetivo;
    private final Reloj reloj;
    private final Planificador planificador;
    private final Memoria memoria;
    private InterrupcionListener listener;

    public Interrupcion(int id, TipoInterrupcion tipo, CPU cpuObjetivo,
                        Reloj reloj, Planificador planificador, Memoria memoria) {
        this.idInterrupcion = id;
        this.tipo = tipo;
        this.cpuObjetivo = cpuObjetivo;
        this.reloj = reloj;
        this.planificador = planificador;
        this.memoria = memoria;
        this.setName("INT-" + id + "-" + tipo.name());
        this.setDaemon(true);
    }

    public void setListener(InterrupcionListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        notificar("[INTERRUPCION] " + tipo.getDescripcion()
                + " en CPU-" + cpuObjetivo.getCpuId()
                + " | ISR: " + tipo.getCiclosISR() + " ciclos");

        // 6.4 — Suspender proceso actual en CPU
        Proceso procesoSuspendido = cpuObjetivo.interrumpir();

        if (procesoSuspendido != null) {
            notificar("[ISR-CPU" + cpuObjetivo.getCpuId() + "] Proceso "
                    + procesoSuspendido.getId() + " suspendido por interrupción");
        } else {
            notificar("[ISR-CPU" + cpuObjetivo.getCpuId()
                    + "] CPU inactiva, ejecutando ISR directamente");
        }

        // 6.5 — Ejecutar ISR sincronizada con el reloj vía semáforo
        notificar("[ISR-CPU" + cpuObjetivo.getCpuId() + "] Ejecutando: "
                + tipo.getDescripcion() + " (" + tipo.getCiclosISR() + " ciclos)");

        // Registrar un semáforo en el reloj para recibir ticks
        Semaphore semISR = reloj.registrarISR();

        try {
            for (int i = 0; i < tipo.getCiclosISR(); i++) {
                semISR.acquire(); // esperar exactamente un tick del reloj
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            notificar("[ISR-CPU" + cpuObjetivo.getCpuId()
                    + "] ISR interrumpida prematuramente");
        } finally {
            // Desregistrar el semáforo del reloj
            reloj.desregistrarISR(semISR);
        }

        notificar("[ISR-CPU" + cpuObjetivo.getCpuId() + "] Completada: "
                + tipo.getDescripcion());

        // 6.7 — El thread regresa al procesador donde fue generado
        if (procesoSuspendido != null && !procesoSuspendido.haTerminado()) {
            cpuObjetivo.restaurarProceso(procesoSuspendido);
            notificar("[ISR-CPU" + cpuObjetivo.getCpuId() + "] Proceso "
                    + procesoSuspendido.getId() + " restaurado en misma CPU");
        } else if (procesoSuspendido != null && procesoSuspendido.haTerminado()) {
            // El proceso ya terminó, mandarlo a terminados
            procesoSuspendido.setEstado(EstadoProceso.TERMINADO);
            procesoSuspendido.detenerHilo();
            memoria.encolarTerminado(procesoSuspendido);
            cpuObjetivo.finalizarInterrupcion();
            notificar("[ISR-CPU" + cpuObjetivo.getCpuId() + "] Proceso "
                    + procesoSuspendido.getId() + " ya terminado, CPU liberada");
        } else {
            cpuObjetivo.finalizarInterrupcion();
            notificar("[ISR-CPU" + cpuObjetivo.getCpuId() + "] CPU liberada");
        }

        // 6.6 — Re-planificar (el planificador lo hace automáticamente en su ciclo)
    }

    private void notificar(String mensaje) {
        System.out.println(mensaje);
        if (listener != null) {
            listener.onEventoInterrupcion(mensaje);
        }
    }

    public TipoInterrupcion getTipo() { return tipo; }
    public int getIdInterrupcion() { return idInterrupcion; }
}
