/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vista;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import modelo.Reloj;
import modelo.Memoria;
import modelo.Proceso;
import modelo.CPU;
import modelo.Planificador;
import modelo.GeneradorProcesos;
import modelo.EstadoProceso;
import planificadores.PoliticaPlanificacion;
import estructuras.Nodo;

/**
 *
 * @author carluchocp
 */
public class Dashboard extends JFrame {

    // --- Referencias del sistema ---
    private Reloj reloj;
    private Memoria memoria;
    private CPU cpu1;
    private CPU cpu2;
    private Planificador planificador;
    private GeneradorProcesos generador;

    // --- Panel superior: Reloj + Indicador SO/Usuario ---
    private JLabel lblReloj;
    private JLabel lblEstadoSistema;
    private JLabel lblAlgoritmoActual;

    // --- CPUs ---
    private JTextArea txtCpu1;
    private JTextArea txtCpu2;

    // --- Colas ---
    private JTextArea txtNuevos;
    private JTextArea txtListos;
    private JTextArea txtBloqueados;
    private JTextArea txtListosSuspendidos;
    private JTextArea txtBloqueadosSuspendidos;
    private JTextArea txtTerminados;

    // --- Log de eventos ---
    private JTextArea txtLog;

    // --- Métricas ---
    private JLabel lblProcesosEnRAM;
    private JLabel lblTotalProcesos;
    private JLabel lblTerminados;
    private JLabel lblDeadlineCumplidos;
    private JLabel lblDeadlineFallidos;
    private JLabel lblTasaExito;

    // --- Controles ---
    private JComboBox<PoliticaPlanificacion> cmbAlgoritmo;
    private JSpinner spnQuantum;
    private JSpinner spnVelocidad;
    private JButton btnIniciar;
    private JButton btnPausar;
    private boolean simulacionIniciada = false;

    // --- Timer UI ---
    private Timer timerUI;

    // --- Contadores de métricas ---
    private int totalTerminados = 0;
    private int deadlineCumplidos = 0;
    private int deadlineFallidos = 0;

    public Dashboard(Reloj reloj, Memoria memoria, CPU cpu1, CPU cpu2,
                     Planificador planificador, GeneradorProcesos generador) {
        this.reloj = reloj;
        this.memoria = memoria;
        this.cpu1 = cpu1;
        this.cpu2 = cpu2;
        this.planificador = planificador;
        this.generador = generador;
        configurarVentana();
        inicializarComponentes();
        iniciarActualizacionUI();
    }

    private void configurarVentana() {
        setTitle("UNIMET-Sat RTOS Simulator");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));
    }

    private void inicializarComponentes() {
        // ===================== PANEL SUPERIOR =====================
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(new Color(30, 30, 30));
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        lblReloj = new JLabel("Ciclo: 0");
        lblReloj.setFont(new Font("Consolas", Font.BOLD, 22));
        lblReloj.setForeground(Color.GREEN);

        lblEstadoSistema = new JLabel("  [Detenido]  ");
        lblEstadoSistema.setFont(new Font("Consolas", Font.BOLD, 16));
        lblEstadoSistema.setForeground(Color.ORANGE);
        lblEstadoSistema.setOpaque(true);
        lblEstadoSistema.setBackground(new Color(50, 50, 50));

        lblAlgoritmoActual = new JLabel("Algoritmo: FCFS");
        lblAlgoritmoActual.setFont(new Font("Consolas", Font.BOLD, 16));
        lblAlgoritmoActual.setForeground(Color.YELLOW);

        JPanel panelIzqSup = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        panelIzqSup.setOpaque(false);
        panelIzqSup.add(lblReloj);
        panelIzqSup.add(lblEstadoSistema);
        panelIzqSup.add(lblAlgoritmoActual);

        // Botones de control en la barra superior
        btnIniciar = new JButton("▶ Iniciar");
        btnIniciar.setFont(new Font("Consolas", Font.BOLD, 14));
        btnIniciar.setBackground(new Color(0, 150, 0));
        btnIniciar.setForeground(Color.WHITE);

        btnPausar = new JButton("⏸ Pausar");
        btnPausar.setFont(new Font("Consolas", Font.BOLD, 14));
        btnPausar.setEnabled(false);

        btnIniciar.addActionListener(e -> iniciarSimulacion());
        btnPausar.addActionListener(e -> togglePausa());

        JPanel panelBotonesSup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBotonesSup.setOpaque(false);
        panelBotonesSup.add(btnIniciar);
        panelBotonesSup.add(btnPausar);

        panelSuperior.add(panelIzqSup, BorderLayout.WEST);
        panelSuperior.add(panelBotonesSup, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        // ===================== PANEL CENTRAL =====================
        JPanel panelCentral = new JPanel(new BorderLayout(5, 5));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // --- CPUs (arriba) ---
        JPanel panelCPUs = new JPanel(new GridLayout(1, 2, 8, 0));
        txtCpu1 = crearTextArea(14, true);
        txtCpu2 = crearTextArea(14, true);
        panelCPUs.add(crearScrollConTitulo(txtCpu1, "CPU 1"));
        panelCPUs.add(crearScrollConTitulo(txtCpu2, "CPU 2"));
        panelCPUs.setPreferredSize(new Dimension(0, 160));

        // --- Colas (centro) ---
        JPanel panelColas = new JPanel(new GridLayout(2, 3, 6, 6));

        txtNuevos = crearTextArea(12, false);
        txtListos = crearTextArea(12, false);
        txtBloqueados = crearTextArea(12, false);
        txtListosSuspendidos = crearTextArea(12, false);
        txtBloqueadosSuspendidos = crearTextArea(12, false);
        txtTerminados = crearTextArea(12, false);

        panelColas.add(crearScrollConTitulo(txtNuevos, "Nuevos"));
        panelColas.add(crearScrollConTitulo(txtListos, "Listos (RAM)"));
        panelColas.add(crearScrollConTitulo(txtBloqueados, "Bloqueados"));
        panelColas.add(crearScrollConTitulo(txtListosSuspendidos, "Listo/Suspendido"));
        panelColas.add(crearScrollConTitulo(txtBloqueadosSuspendidos, "Bloqueado/Suspendido"));
        panelColas.add(crearScrollConTitulo(txtTerminados, "Terminados"));

        panelCentral.add(panelCPUs, BorderLayout.NORTH);
        panelCentral.add(panelColas, BorderLayout.CENTER);
        add(panelCentral, BorderLayout.CENTER);

        // ===================== PANEL DERECHO (Controles + Métricas) =====================
        JPanel panelDerecho = new JPanel();
        panelDerecho.setLayout(new BoxLayout(panelDerecho, BoxLayout.Y_AXIS));
        panelDerecho.setPreferredSize(new Dimension(280, 0));
        panelDerecho.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // --- Controles de algoritmo ---
        JPanel panelAlgoritmo = new JPanel(new GridLayout(0, 1, 4, 4));
        panelAlgoritmo.setBorder(BorderFactory.createTitledBorder("Planificación"));

        cmbAlgoritmo = new JComboBox<>(PoliticaPlanificacion.values());
        cmbAlgoritmo.setFont(new Font("Consolas", Font.PLAIN, 12));
        panelAlgoritmo.add(new JLabel("Algoritmo:"));
        panelAlgoritmo.add(cmbAlgoritmo);

        JPanel panelQuantum = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelQuantum.add(new JLabel("Quantum:"));
        spnQuantum = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        panelQuantum.add(spnQuantum);
        panelAlgoritmo.add(panelQuantum);

        JButton btnCambiarAlg = new JButton("Cambiar Algoritmo");
        btnCambiarAlg.addActionListener(e -> {
            PoliticaPlanificacion pol = (PoliticaPlanificacion) cmbAlgoritmo.getSelectedItem();
            int quantum = (int) spnQuantum.getValue();
            planificador.cambiarAlgoritmo(pol, quantum);
            agregarLog("Algoritmo cambiado a: " + pol.getDescripcion());
        });
        panelAlgoritmo.add(btnCambiarAlg);
        panelDerecho.add(panelAlgoritmo);

        // --- Velocidad del reloj ---
        JPanel panelVelocidad = new JPanel(new GridLayout(0, 1, 4, 4));
        panelVelocidad.setBorder(BorderFactory.createTitledBorder("Velocidad"));
        JPanel panelSpnVel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelSpnVel.add(new JLabel("Ciclo (ms):"));
        spnVelocidad = new JSpinner(new SpinnerNumberModel(
                reloj.getDuracionCiclo(), 50, 5000, 50));
        spnVelocidad.addChangeListener(e -> {
            int ms = (int) spnVelocidad.getValue();
            reloj.setDuracionCiclo(ms);
            agregarLog("Velocidad cambiada a: " + ms + " ms/ciclo");
        });
        panelSpnVel.add(spnVelocidad);
        panelVelocidad.add(panelSpnVel);
        panelDerecho.add(panelVelocidad);

        // --- Acciones ---
        JPanel panelAcciones = new JPanel(new GridLayout(0, 1, 4, 4));
        panelAcciones.setBorder(BorderFactory.createTitledBorder("Acciones"));

        JButton btnGenerar20 = new JButton("Generar 20 Procesos");
        btnGenerar20.addActionListener(e -> {
            generador.generarProcesosIniciales(memoria, 20);
            agregarLog("20 procesos aleatorios generados");
        });
        panelAcciones.add(btnGenerar20);

        JButton btnGenerar1 = new JButton("Tarea de Emergencia");
        btnGenerar1.addActionListener(e -> {
            Proceso p = generador.crearProcesoAleatorio();
            memoria.admitirProceso(p);
            agregarLog("Emergencia: " + p.getId() + " creado");
        });
        panelAcciones.add(btnGenerar1);
        panelDerecho.add(panelAcciones);

        // --- Métricas ---
        JPanel panelMetricas = new JPanel(new GridLayout(0, 1, 2, 2));
        panelMetricas.setBorder(BorderFactory.createTitledBorder("Métricas"));

        lblProcesosEnRAM = new JLabel("En RAM: 0 / 0");
        lblTotalProcesos = new JLabel("Total procesos: 0");
        lblTerminados = new JLabel("Terminados: 0");
        lblDeadlineCumplidos = new JLabel("Deadline OK: 0");
        lblDeadlineFallidos = new JLabel("Deadline FAIL: 0");
        lblTasaExito = new JLabel("Tasa éxito: 0%");

        Font fontMetrica = new Font("Consolas", Font.PLAIN, 12);
        lblProcesosEnRAM.setFont(fontMetrica);
        lblTotalProcesos.setFont(fontMetrica);
        lblTerminados.setFont(fontMetrica);
        lblDeadlineCumplidos.setFont(fontMetrica);
        lblDeadlineFallidos.setFont(fontMetrica);
        lblTasaExito.setFont(fontMetrica);
        lblTasaExito.setForeground(new Color(0, 150, 0));

        panelMetricas.add(lblProcesosEnRAM);
        panelMetricas.add(lblTotalProcesos);
        panelMetricas.add(lblTerminados);
        panelMetricas.add(lblDeadlineCumplidos);
        panelMetricas.add(lblDeadlineFallidos);
        panelMetricas.add(lblTasaExito);
        panelDerecho.add(panelMetricas);

        add(panelDerecho, BorderLayout.EAST);

        // ===================== PANEL INFERIOR (Log) =====================
        txtLog = new JTextArea(6, 0);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtLog.setBackground(new Color(20, 20, 20));
        txtLog.setForeground(Color.GREEN);
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));
        add(scrollLog, BorderLayout.SOUTH);
    }

    // ======================== Control de simulación ========================

    private void iniciarSimulacion() {
        if (!simulacionIniciada) {
            // Aplicar algoritmo seleccionado antes de iniciar
            PoliticaPlanificacion pol = (PoliticaPlanificacion) cmbAlgoritmo.getSelectedItem();
            int quantum = (int) spnQuantum.getValue();
            planificador.cambiarAlgoritmo(pol, quantum);

            simulacionIniciada = true;
            btnIniciar.setEnabled(false);
            btnPausar.setEnabled(true);
            agregarLog("Simulación iniciada con " + pol.getDescripcion());
        }

        // Reanudar todos los componentes
        planificador.reanudar();
        cpu1.reanudar();
        cpu2.reanudar();

        lblEstadoSistema.setText("  [Ejecutando]  ");
        lblEstadoSistema.setForeground(Color.GREEN);
    }

    private void togglePausa() {
        if (planificador.isPausado()) {
            planificador.reanudar();
            cpu1.reanudar();
            cpu2.reanudar();
            btnPausar.setText("⏸ Pausar");
            lblEstadoSistema.setText("  [Ejecutando]  ");
            lblEstadoSistema.setForeground(Color.GREEN);
            agregarLog("Simulación reanudada");
        } else {
            planificador.pausar();
            cpu1.pausar();
            cpu2.pausar();
            btnPausar.setText("▶ Reanudar");
            lblEstadoSistema.setText("  [Pausado]  ");
            lblEstadoSistema.setForeground(Color.ORANGE);
            agregarLog("Simulación pausada");
        }
    }

    // ======================== Helpers UI ========================

    private JTextArea crearTextArea(int fontSize, boolean bold) {
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font("Consolas", bold ? Font.BOLD : Font.PLAIN, fontSize));
        return ta;
    }

    private JScrollPane crearScrollConTitulo(JTextArea ta, String titulo) {
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), titulo,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Consolas", Font.BOLD, 12)));
        return sp;
    }

    public void agregarLog(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append("[Ciclo " + reloj.getCicloGlobal() + "] " + mensaje + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    // ======================== Actualización periódica ========================

    private void iniciarActualizacionUI() {
        timerUI = new Timer(100, e -> {
            lblReloj.setText("Ciclo: " + reloj.getCicloGlobal());
            lblAlgoritmoActual.setText("Algoritmo: " + planificador.getPoliticaActual());
            if (simulacionIniciada && !planificador.isPausado()) {
                actualizarIndicadorSistema();
            }
            actualizarCPUs();
            actualizarColas();
            actualizarMetricas();
        });
        timerUI.start();
    }

    private void actualizarIndicadorSistema() {
        boolean soActivo = (cpu1.getProcesoActual() == null && !memoria.getColaListos().estaVacia())
                        || (cpu2.getProcesoActual() == null && !memoria.getColaListos().estaVacia());
        if (soActivo) {
            lblEstadoSistema.setText("  [S.O.]  ");
            lblEstadoSistema.setForeground(Color.RED);
        } else {
            lblEstadoSistema.setText("  [Usuario]  ");
            lblEstadoSistema.setForeground(Color.CYAN);
        }
    }

    private void actualizarCPUs() {
        txtCpu1.setText(formatearCPU(cpu1, 1));
        txtCpu2.setText(formatearCPU(cpu2, 2));
    }

    private String formatearCPU(CPU cpu, int numCpu) {
        Proceso p = cpu.getProcesoActual();
        if (p == null) {
            return "\n  [CPU " + numCpu + " Inactiva - Esperando proceso]";
        }
        return String.format(
            "\n  %s (%s)\n" +
            "  Estado: %s | Prioridad: %d\n" +
            "  PC: %d / %d | MAR: %d\n" +
            "  Deadline: %d (rest: %d)\n" +
            "  Tipo: %s | %s | Quantum: %d",
            p.getNombre(), p.getId(),
            p.getEstado(), p.getPrioridad(),
            p.getPc(), p.getInstruccionesTotales(), p.getMar(),
            p.getDeadline(), p.getTiempoRestanteDeadline(),
            p.isCpuBound() ? "CPU-Bound" : "IO-Bound",
            p.isPeriodico() ? "Periódico(T=" + p.getPeriodo() + ")" : "Aperiódico",
            cpu.getCiclosEnQuantum()
        );
    }

    private void actualizarColas() {
        txtNuevos.setText(formatearCola(memoria.getColaNuevos()));
        txtListos.setText(formatearCola(memoria.getColaListos()));
        txtBloqueados.setText(formatearColaBloqueados(memoria.getColaBloqueados()));
        txtListosSuspendidos.setText(formatearCola(memoria.getColaListosSuspendidos()));
        txtBloqueadosSuspendidos.setText(formatearCola(memoria.getColaBloqueadosSuspendidos()));
        txtTerminados.setText(formatearColaSimple(memoria.getColaTerminados()));
    }

    private String formatearCola(estructuras.Cola<Proceso> cola) {
        StringBuilder sb = new StringBuilder();
        Nodo<Proceso> actual = cola.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            sb.append(String.format("[%s] %s | P:%d | PC:%d/%d | DL:%d\n",
                    p.getId(), p.getNombre(), p.getPrioridad(),
                    p.getPc(), p.getInstruccionesTotales(),
                    p.getTiempoRestanteDeadline()));
            actual = actual.getSiguiente();
        }
        if (sb.length() == 0) sb.append("(vacía)");
        return sb.toString();
    }

    private String formatearColaBloqueados(estructuras.Cola<Proceso> cola) {
        StringBuilder sb = new StringBuilder();
        Nodo<Proceso> actual = cola.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            sb.append(String.format("[%s] %s | E/S rest: %d | DL:%d\n",
                    p.getId(), p.getNombre(),
                    p.getCiclosESRestantes(),
                    p.getTiempoRestanteDeadline()));
            actual = actual.getSiguiente();
        }
        if (sb.length() == 0) sb.append("(vacía)");
        return sb.toString();
    }

    private String formatearColaSimple(estructuras.Cola<Proceso> cola) {
        StringBuilder sb = new StringBuilder();
        Nodo<Proceso> actual = cola.getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            sb.append(String.format("[%s] %s | Instr: %d\n",
                    p.getId(), p.getNombre(), p.getInstruccionesTotales()));
            actual = actual.getSiguiente();
        }
        if (sb.length() == 0) sb.append("(vacía)");
        return sb.toString();
    }

    private void actualizarMetricas() {
        int enRAM = memoria.getProcesosEnRAM();
        int capMax = memoria.getCapacidadMaxima();
        int terminados = memoria.getColaTerminados().getTamano();

        // Contar deadlines cumplidos/fallidos de terminados
        int cumplidos = 0;
        int fallidos = 0;
        Nodo<Proceso> actual = memoria.getColaTerminados().getPrimerNodo();
        while (actual != null) {
            Proceso p = actual.getContenido();
            if (p.getTiempoRestanteDeadline() >= 0) {
                cumplidos++;
            } else {
                fallidos++;
            }
            actual = actual.getSiguiente();
        }

        int totalEnSistema = memoria.getColaNuevos().getTamano()
                + memoria.getColaListos().getTamano()
                + memoria.getColaBloqueados().getTamano()
                + memoria.getColaListosSuspendidos().getTamano()
                + memoria.getColaBloqueadosSuspendidos().getTamano()
                + terminados
                + (cpu1.getProcesoActual() != null ? 1 : 0)
                + (cpu2.getProcesoActual() != null ? 1 : 0);

        double tasa = terminados > 0 ? (cumplidos * 100.0 / terminados) : 0;

        lblProcesosEnRAM.setText("En RAM: " + enRAM + " / " + capMax);
        lblTotalProcesos.setText("Total procesos: " + totalEnSistema);
        lblTerminados.setText("Terminados: " + terminados);
        lblDeadlineCumplidos.setText("Deadline OK: " + cumplidos);
        lblDeadlineFallidos.setText("Deadline FAIL: " + fallidos);
        lblTasaExito.setText(String.format("Tasa éxito: %.1f%%", tasa));
        lblTasaExito.setForeground(tasa >= 80 ? new Color(0, 150, 0) : Color.RED);
    }
}
