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

    // --- Identificación ---
    private String id;
    private String nombre;

    // --- Estado y registros ---
    private EstadoProceso estado;
    private int pc;                     // Program Counter
    private int mar;                    // Memory Address Register

    // --- Planificación ---
    private int prioridad;
    private int deadline;               // en ciclos
    private int tiempoRestanteDeadline;

    // --- Instrucciones ---
    private int instruccionesTotales;
    private int instruccionesEjecutadas;

    // --- Tipo y periodicidad ---
    private boolean periodico;          // true = periódico, false = aperiódico
    private int periodo;                // solo aplica si es periódico

    // --- E/S ---
    private int ciclosParaES;           // cada cuántos ciclos requiere E/S
    private int ciclosESRestantes;      // ciclos que faltan para terminar E/S actual

    // --- CPU vs E/S ---
    private boolean cpuBound;           // true = CPU-bound, false = IO-bound

    // ======================== Constructor ========================

    public Proceso(String id, String nombre, int instruccionesTotales,
                   int prioridad, int deadline,
                   boolean periodico, int periodo,
                   int ciclosParaES, boolean cpuBound) {
        this.id = id;
        this.nombre = nombre;
        this.instruccionesTotales = instruccionesTotales;
        this.prioridad = prioridad;
        this.deadline = deadline;
        this.tiempoRestanteDeadline = deadline;
        this.periodico = periodico;
        this.periodo = periodico ? periodo : 0;
        this.ciclosParaES = ciclosParaES;
        this.cpuBound = cpuBound;

        this.estado = EstadoProceso.NUEVO;
        this.pc = 0;
        this.mar = 0;
        this.instruccionesEjecutadas = 0;
        this.ciclosESRestantes = 0;
    }

    // ======================== Transición validada ========================

    public void setEstado(EstadoProceso nuevoEstado) {
        if (this.estado == nuevoEstado) {
            return; // ya está en ese estado, no hacer nada
        }

        if (!this.estado.puedeTransicionarA(nuevoEstado)) {
            throw new IllegalStateException(
                String.format("Transición inválida: %s -> %s (Proceso %s)",
                    this.estado, nuevoEstado, this.id));
        }
        this.estado = nuevoEstado;
    }

    // ======================== Getters y Setters ========================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public EstadoProceso getEstado() { return estado; }

    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }

    public int getMar() { return mar; }
    public void setMar(int mar) { this.mar = mar; }

    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }

    public int getDeadline() { return deadline; }
    public void setDeadline(int deadline) { this.deadline = deadline; }

    public int getTiempoRestanteDeadline() { return tiempoRestanteDeadline; }
    public void setTiempoRestanteDeadline(int t) { this.tiempoRestanteDeadline = t; }

    public int getInstruccionesTotales() { return instruccionesTotales; }
    public void setInstruccionesTotales(int i) { this.instruccionesTotales = i; }

    public int getInstruccionesEjecutadas() { return instruccionesEjecutadas; }
    public void setInstruccionesEjecutadas(int i) { this.instruccionesEjecutadas = i; }

    public boolean isPeriodico() { return periodico; }
    public void setPeriodico(boolean periodico) { this.periodico = periodico; }

    public int getPeriodo() { return periodo; }
    public void setPeriodo(int periodo) { this.periodo = periodo; }

    public int getCiclosParaES() { return ciclosParaES; }
    public void setCiclosParaES(int c) { this.ciclosParaES = c; }

    public int getCiclosESRestantes() { return ciclosESRestantes; }
    public void setCiclosESRestantes(int c) { this.ciclosESRestantes = c; }

    public boolean isCpuBound() { return cpuBound; }
    public void setCpuBound(boolean cpuBound) { this.cpuBound = cpuBound; }

    // ======================== Métodos utilitarios ========================

    public void avanzarCiclo() {
        this.pc++;
        this.instruccionesEjecutadas++;
    }

    public boolean haTerminado() {
        return instruccionesEjecutadas >= instruccionesTotales;
    }

    public boolean necesitaES() {
        return ciclosParaES > 0
                && instruccionesEjecutadas > 0
                && instruccionesEjecutadas % ciclosParaES == 0
                && !haTerminado();
    }

    public boolean deadlineExpirado() {
        return tiempoRestanteDeadline <= 0;
    }

    @Override
    public String toString() {
        return String.format(
            "Proceso[id=%s, nombre='%s', estado=%s, PC=%d, MAR=%d, prioridad=%d, "
            + "deadline=%d, deadlineRest=%d, instr=%d/%d, periodico=%b, periodo=%d, "
            + "ciclosES=%d, ciclosESRest=%d, cpuBound=%b]",
            id, nombre, estado, pc, mar, prioridad,
            deadline, tiempoRestanteDeadline,
            instruccionesEjecutadas, instruccionesTotales,
            periodico, periodo, ciclosParaES, ciclosESRestantes, cpuBound);
    }
}
