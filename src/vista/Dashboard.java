/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vista;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Color;
import modelo.Reloj;
import modelo.Memoria;
import modelo.Proceso;
import modelo.CPU;
import estructuras.Nodo;

public class Dashboard extends JFrame {
    
    private JLabel lblReloj;
    private JTextArea txtCpu1;
    private JTextArea txtCpu2;
    private JTextArea txtListos;
    private JTextArea txtBloqueados;
    private Reloj reloj;
    private Memoria memoria;
    private CPU cpu1;
    private CPU cpu2;
    private Timer timerUI;

    public Dashboard(Reloj reloj, Memoria memoria, CPU cpu1, CPU cpu2) {
        this.reloj = reloj;
        this.memoria = memoria;
        this.cpu1 = cpu1;
        this.cpu2 = cpu2;
        configurarVentana();
        inicializarComponentes();
        iniciarActualizacionUI();
    }

    private void configurarVentana() {
        setTitle("UNIMET-Sat RTOS Simulator");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void inicializarComponentes() {
        JPanel panelSuperior = new JPanel();
        panelSuperior.setBackground(Color.DARK_GRAY);
        lblReloj = new JLabel("Ciclo de Reloj: 0");
        lblReloj.setFont(new Font("Consolas", Font.BOLD, 24));
        lblReloj.setForeground(Color.WHITE);
        panelSuperior.add(lblReloj);
        add(panelSuperior, BorderLayout.NORTH);

        JPanel panelPrincipal = new JPanel(new GridLayout(2, 1, 10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelCPUs = new JPanel(new GridLayout(1, 2, 10, 10));
        txtCpu1 = new JTextArea();
        txtCpu1.setEditable(false);
        txtCpu1.setFont(new Font("Consolas", Font.BOLD, 16));
        JScrollPane scrollCpu1 = new JScrollPane(txtCpu1);
        scrollCpu1.setBorder(BorderFactory.createTitledBorder("CPU 1 - Ejecución"));

        txtCpu2 = new JTextArea();
        txtCpu2.setEditable(false);
        txtCpu2.setFont(new Font("Consolas", Font.BOLD, 16));
        JScrollPane scrollCpu2 = new JScrollPane(txtCpu2);
        scrollCpu2.setBorder(BorderFactory.createTitledBorder("CPU 2 - Ejecución"));

        panelCPUs.add(scrollCpu1);
        panelCPUs.add(scrollCpu2);

        JPanel panelColas = new JPanel(new GridLayout(1, 2, 10, 10));
        txtListos = new JTextArea();
        txtListos.setEditable(false);
        txtListos.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane scrollListos = new JScrollPane(txtListos);
        scrollListos.setBorder(BorderFactory.createTitledBorder("Cola de Listos (RAM)"));

        txtBloqueados = new JTextArea();
        txtBloqueados.setEditable(false);
        txtBloqueados.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane scrollBloqueados = new JScrollPane(txtBloqueados);
        scrollBloqueados.setBorder(BorderFactory.createTitledBorder("Cola de Bloqueados"));

        panelColas.add(scrollListos);
        panelColas.add(scrollBloqueados);

        panelPrincipal.add(panelCPUs);
        panelPrincipal.add(panelColas);

        add(panelPrincipal, BorderLayout.CENTER);
    }

    private void iniciarActualizacionUI() {
        timerUI = new Timer(100, e -> {
            lblReloj.setText("Ciclo de Reloj: " + reloj.getCicloGlobal());
            actualizarCPUs();
            actualizarColas();
        });
        timerUI.start();
    }

    private void actualizarCPUs() {
        Proceso p1 = cpu1.getProcesoActual();
        if (p1 != null) {
            txtCpu1.setText("\n  Proceso: " + p1.getNombre() + "\n" +
                            "  ID: " + p1.getId() + "\n" +
                            "  PC: " + p1.getPc() + " / " + p1.getInstruccionesTotales() + "\n" +
                            "  Tipo: " + (p1.isCpuBound() ? "CPU" : "E/S"));
        } else {
            txtCpu1.setText("\n  [Inactiva - Esperando proceso]");
        }

        Proceso p2 = cpu2.getProcesoActual();
        if (p2 != null) {
            txtCpu2.setText("\n  Proceso: " + p2.getNombre() + "\n" +
                            "  ID: " + p2.getId() + "\n" +
                            "  PC: " + p2.getPc() + " / " + p2.getInstruccionesTotales() + "\n" +
                            "  Tipo: " + (p2.isCpuBound() ? "CPU" : "E/S"));
        } else {
            txtCpu2.setText("\n  [Inactiva - Esperando proceso]");
        }
    }

    private void actualizarColas() {
        StringBuilder sbListos = new StringBuilder();
        Nodo<Proceso> actualListo = memoria.getColaListos().getPrimerNodo();
        while (actualListo != null) {
            Proceso p = actualListo.getContenido();
            sbListos.append("[").append(p.getId()).append("] ")
                    .append(p.getNombre()).append(" | PC: ")
                    .append(p.getPc()).append("/").append(p.getInstruccionesTotales())
                    .append("\n");
            actualListo = actualListo.getSiguiente();
        }
        txtListos.setText(sbListos.toString());

        StringBuilder sbBloqueados = new StringBuilder();
        Nodo<Proceso> actualBloqueado = memoria.getColaBloqueados().getPrimerNodo();
        while (actualBloqueado != null) {
            Proceso p = actualBloqueado.getContenido();
            sbBloqueados.append("[").append(p.getId()).append("] ")
                        .append(p.getNombre())
                        .append("\n");
            actualBloqueado = actualBloqueado.getSiguiente();
        }
        txtBloqueados.setText(sbBloqueados.toString());
    }
}
