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

        Reloj reloj = new Reloj();

        CPU cpu1 = new CPU(1, memoria, reloj);
        CPU cpu2 = new CPU(2, memoria, reloj);

        // 5.4 — Inyectar semáforos del reloj a cada CPU
        cpu1.setSemReloj(reloj.getSemCpu1());
        cpu2.setSemReloj(reloj.getSemCpu2());

        Planificador planificador = new Planificador(memoria, reloj, cpu1, cpu2);

        cpu1.setPlanificador(planificador);
        cpu2.setPlanificador(planificador);

        GeneradorInterrupciones generadorInterrupciones =
                new GeneradorInterrupciones(cpu1, cpu2, reloj, planificador, memoria);

        Dashboard dashboard = new Dashboard(reloj, memoria, cpu1, cpu2,
                planificador, generador, generadorInterrupciones);
        dashboard.setVisible(true);

        generadorInterrupciones.setListener(dashboard);

        // Iniciar todos los hilos del sistema
        reloj.start();
        planificador.start();
        cpu1.start();
        cpu2.start();
        generadorInterrupciones.start();
    }
}
