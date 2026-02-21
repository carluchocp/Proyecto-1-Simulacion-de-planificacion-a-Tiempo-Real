/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author carluchocp
 */
public enum TipoInterrupcion {
    MICRO_METEORITO("Impacto de Micro-Meteorito", 3),
    RAFAGA_SOLAR("Ráfaga Solar Detectada", 4),
    FALLO_SENSOR("Fallo en Sensor de Altitud", 2),
    COMANDO_TIERRA("Comando desde Estación Terrestre", 3),
    SOBRECALENTAMIENTO("Alerta de Sobrecalentamiento", 5),
    PERDIDA_SENAL("Pérdida Momentánea de Señal", 2);

    private final String descripcion;
    private final int ciclosISR;

    TipoInterrupcion(String descripcion, int ciclosISR) {
        this.descripcion = descripcion;
        this.ciclosISR = ciclosISR;
    }

    public String getDescripcion() { return descripcion; }
    public int getCiclosISR() { return ciclosISR; }
}
