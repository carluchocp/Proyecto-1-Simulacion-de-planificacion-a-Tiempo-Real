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
import estructuras.Nodo;

public class Dashboard extends JFrame {
    
    private JLabel lblReloj;
    private JTextArea txtListos;
    private JTextArea txtBloqueados;
    private Reloj reloj;
    private Memoria memoria;
    private Timer timerUI;

    public Dashboard(Reloj reloj, Memoria memoria) {
        this.reloj = reloj;
        this.memoria = memoria;
        configurarVentana();
        inicializarComponentes();
        iniciarActualizacionUI();
    }

    private void configurarVentana() {
        setTitle("UNIMET-Sat RTOS Simulator");
        setSize(900, 600);
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

        JPanel panelCentral = new JPanel();
        panelCentral.setLayout(new GridLayout(1, 2, 10, 10));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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

        panelCentral.add(scrollListos);
        panelCentral.add(scrollBloqueados);

        add(panelCentral, BorderLayout.CENTER);
    }

    private void iniciarActualizacionUI() {
        timerUI = new Timer(100, e -> {
            lblReloj.setText("Ciclo de Reloj: " + reloj.getCicloGlobal());
            actualizarColas();
        });
        timerUI.start();
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
