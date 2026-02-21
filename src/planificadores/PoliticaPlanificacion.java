/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package planificadores;

/**
 *
 * @author carluchocp
 */
public enum PoliticaPlanificacion {
    FCFS("First Come First Served"),
    ROUND_ROBIN("Round Robin"),
    SRT("Shortest Remaining Time"),
    PRIORIDAD("Prioridad Estática Preemptiva"),
    EDF("Earliest Deadline First");

    private final String descripcion;

    PoliticaPlanificacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
