/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vista;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Font;
import modelo.Reloj;

public class Dashboard extends JFrame {
    
    private JLabel lblReloj;
    private Reloj reloj;
    private Timer timerUI;

    public Dashboard(Reloj reloj) {
        this.reloj = reloj;
        configurarVentana();
        inicializarComponentes();
        iniciarActualizacionUI();
    }

    private void configurarVentana() {
        setTitle("UNIMET-Sat RTOS Simulator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
    }

    private void inicializarComponentes() {
        JPanel panelSuperior = new JPanel();
        lblReloj = new JLabel("Ciclo de Reloj: 0");
        lblReloj.setFont(new Font("Arial", Font.BOLD, 24));
        panelSuperior.add(lblReloj);
        add(panelSuperior, BorderLayout.NORTH);
    }

    private void iniciarActualizacionUI() {
        timerUI = new Timer(100, e -> {
            lblReloj.setText("Ciclo de Reloj: " + reloj.getCicloGlobal());
        });
        timerUI.start();
    }
}
