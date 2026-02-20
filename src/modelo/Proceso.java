/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author diego
 */
public class Proceso {
    
    private String id;
    private String nombre;
    private int instruccionesTotales;
    private String tipoRequerimiento;
    private int deadline;
    private int prioridad;
    private int pc;
    private EstadoProceso estado;

    public Proceso(String id, String nombre, int instruccionesTotales, String tipoRequerimiento, int deadline, int prioridad) {
        this.id = id;
        this.nombre = nombre;
        this.instruccionesTotales = instruccionesTotales;
        this.tipoRequerimiento = tipoRequerimiento;
        this.deadline = deadline;
        this.prioridad = prioridad;
        this.pc = 0;
        this.estado = EstadoProceso.NUEVO;
    }

    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public void avanzarCiclo() {
        this.pc++;
    }

    public int getPc() {
        return pc;
    }

    public int getInstruccionesTotales() {
        return instruccionesTotales;
    }

    public String getTipoRequerimiento() {
        return tipoRequerimiento;
    }
    
    public String getId() {
        return id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public int getDeadline() {
        return deadline;
    }
    
    public int getPrioridad() {
        return prioridad;
    }
}
