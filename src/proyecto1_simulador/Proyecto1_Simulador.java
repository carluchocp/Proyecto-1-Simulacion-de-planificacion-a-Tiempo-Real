/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package proyecto1_simulador;

import modelo.Memoria;
import modelo.GeneradorProcesos;
import modelo.Reloj;
import modelo.CPU;
import modelo.Planificador;
import modelo.GeneradorInterrupciones;
import vista.Dashboard;

public class Proyecto1_Simulador {

    public static void main(String[] args) {

        Memoria memoria = new Memoria(10);

        GeneradorProcesos generador = new GeneradorProcesos();
        generador.generarProcesosIniciales(memoria, 20);

        Reloj reloj = new Reloj();

        CPU cpu1 = new CPU(1, memoria, reloj);
        CPU cpu2 = new CPU(2, memoria, reloj);

        Planificador planificador = new Planificador(memoria, reloj, cpu1, cpu2);

        GeneradorInterrupciones generadorInterrupciones =
                new GeneradorInterrupciones(cpu1, cpu2, reloj, planificador);

        Dashboard dashboard = new Dashboard(reloj, memoria, cpu1, cpu2,
                planificador, generador, generadorInterrupciones);
        dashboard.setVisible(true);

        generadorInterrupciones.setListener(dashboard);

        reloj.start();
        planificador.start();
        cpu1.start();
        cpu2.start();
        generadorInterrupciones.start();
    }
}
