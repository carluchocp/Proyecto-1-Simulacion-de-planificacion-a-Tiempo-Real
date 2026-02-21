/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package planificadores;

import estructuras.Cola;
import estructuras.Nodo;
import modelo.Proceso;

/**
 *
 * @author carluchocp
 */
public class EDF implements AlgoritmoPlanificacion {

    @Override
    public Proceso seleccionarProceso(Cola<Proceso> colaListos) {
        if (colaListos.estaVacia()) {
            return null;
        }

        Proceso mejor = null;
        int menorDeadline = Integer.MAX_VALUE;

        Nodo<Proceso> actual = colaListos.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            if (p.getTiempoRestanteDeadline() < menorDeadline) {
                menorDeadline = p.getTiempoRestanteDeadline();
                mejor = p;
            }
            actual = actual.getSiguiente();
        }

        if (mejor != null) {
            removerDeCola(colaListos, mejor);
        }
        return mejor;
    }

    @Override
    public boolean debePreemptar(Proceso enEjecucion, Cola<Proceso> colaListos) {
        if (enEjecucion == null || colaListos.estaVacia()) {
            return false;
        }

        Nodo<Proceso> actual = colaListos.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            if (p.getTiempoRestanteDeadline() < enEjecucion.getTiempoRestanteDeadline()) {
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    private void removerDeCola(Cola<Proceso> cola, Proceso objetivo) {
        Cola<Proceso> temp = new Cola<>();
        Proceso p;
        boolean removido = false;
        while ((p = cola.desencolar()) != null) {
            if (p.getId().equals(objetivo.getId()) && !removido) {
                removido = true;
            } else {
                temp.encolar(p);
            }
        }
        while ((p = temp.desencolar()) != null) {
            cola.encolar(p);
        }
    }

    @Override
    public boolean usaQuantum() { return false; }

    @Override
    public int getQuantum() { return 0; }

    @Override
    public PoliticaPlanificacion getPolitica() {
        return PoliticaPlanificacion.EDF;
    }
}
