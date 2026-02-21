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
import modelo.GeneradorInterrupciones;
import modelo.InterrupcionListener;
import modelo.TipoInterrupcion;
import modelo.Interrupcion;
import modelo.Metricas;
import planificadores.PoliticaPlanificacion;
import estructuras.Nodo;

public class Dashboard extends JFrame implements InterrupcionListener {

    // --- Referencias del sistema ---
    private Reloj reloj;
    private Memoria memoria;
    private CPU cpu1;
    private CPU cpu2;
    private Planificador planificador;
    private GeneradorProcesos generador;
    private GeneradorInterrupciones generadorInterrupciones;

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
    private JLabel lblInterrupciones;
    private JLabel lblThroughput;
    private JLabel lblTiempoEsperaPromedio;
    private JLabel lblUtilizacionCPU;

    // --- Gráfico 9.1 (custom) ---
    private GraficoUtilizacionPanel panelGrafico;
    private int ultimoIndiceGrafico = 0;

    // --- Controles ---
    private JComboBox<PoliticaPlanificacion> cmbAlgoritmo;
    private JSpinner spnQuantum;
    private JSpinner spnVelocidad;
    private JButton btnIniciar;
    private JButton btnPausar;
    private JButton btnReiniciar;
    private boolean simulacionIniciada = false;

    // --- Timer UI ---
    private Timer timerUI;

    // --- Contadores de métricas (legacy, now delegated to Metricas) ---
    private int totalTerminados = 0;
    private int deadlineCumplidos = 0;
    private int deadlineFallidos = 0;

    // --- Tracking de deadlines ya reportados en log ---
    private int ultimoDeadlineFallidoReportado = 0;

    // ======================== Panel de gráfico custom ========================

    /**
     * Panel que dibuja el gráfico de utilización CPU vs tiempo usando Graphics2D.
     */
    private static class GraficoUtilizacionPanel extends JPanel {
        private double[] datos;
        private int[] ciclos;
        private int cantidad;
        private int capacidad;

        public GraficoUtilizacionPanel() {
            this.capacidad = 500;
            this.datos = new double[capacidad];
            this.ciclos = new int[capacidad];
            this.cantidad = 0;
            setBackground(new Color(20, 20, 20));
        }

        public synchronized void agregarPunto(int ciclo, double utilizacion) {
            if (cantidad >= capacidad) {
                // Desplazar: quitar el más viejo
                System.arraycopy(datos, 1, datos, 0, capacidad - 1);
                System.arraycopy(ciclos, 1, ciclos, 0, capacidad - 1);
                cantidad = capacidad - 1;
            }
            datos[cantidad] = utilizacion;
            ciclos[cantidad] = ciclo;
            cantidad++;
        }

        public synchronized void limpiar() {
            cantidad = 0;
        }

        @Override
        protected synchronized void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int margenIzq = 45;
            int margenDer = 10;
            int margenSup = 20;
            int margenInf = 25;
            int areaW = w - margenIzq - margenDer;
            int areaH = h - margenSup - margenInf;

            if (areaW <= 0 || areaH <= 0) return;

            // Título
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.drawString("Utilización CPU vs Tiempo", margenIzq + 10, 14);

            // Ejes
            g2.setColor(Color.DARK_GRAY);
            g2.drawLine(margenIzq, margenSup, margenIzq, margenSup + areaH);
            g2.drawLine(margenIzq, margenSup + areaH, margenIzq + areaW, margenSup + areaH);

            // Etiquetas eje Y
            g2.setFont(new Font("Consolas", Font.PLAIN, 9));
            g2.setColor(Color.LIGHT_GRAY);
            for (int pct = 0; pct <= 100; pct += 25) {
                int y = margenSup + areaH - (int)(areaH * pct / 100.0);
                g2.setColor(new Color(50, 50, 50));
                g2.drawLine(margenIzq, y, margenIzq + areaW, y);
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString(pct + "%", 5, y + 4);
            }

            // Etiqueta eje X
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("Ciclo", margenIzq + areaW / 2 - 15, h - 3);

            if (cantidad < 2) return;

            // Dibujar línea de datos
            g2.setColor(new Color(0, 200, 100));
            g2.setStroke(new BasicStroke(2.0f));

            for (int i = 1; i < cantidad; i++) {
                int x1 = margenIzq + (int)((i - 1) * (double) areaW / (cantidad - 1));
                int x2 = margenIzq + (int)(i * (double) areaW / (cantidad - 1));
                int y1 = margenSup + areaH - (int)(areaH * datos[i - 1] / 100.0);
                int y2 = margenSup + areaH - (int)(areaH * datos[i] / 100.0);
                g2.drawLine(x1, y1, x2, y2);
            }

            // Etiquetas de ciclo en eje X (inicio y fin)
            g2.setColor(Color.LIGHT_GRAY);
            g2.setFont(new Font("Consolas", Font.PLAIN, 9));
            g2.drawString(String.valueOf(ciclos[0]), margenIzq, margenSup + areaH + 15);
            String finStr = String.valueOf(ciclos[cantidad - 1]);
            int finW = g2.getFontMetrics().stringWidth(finStr);
            g2.drawString(finStr, margenIzq + areaW - finW, margenSup + areaH + 15);
        }
    }

    public Dashboard(Reloj reloj, Memoria memoria, CPU cpu1, CPU cpu2,
                     Planificador planificador, GeneradorProcesos generador,
                     GeneradorInterrupciones generadorInterrupciones) {
        this.reloj = reloj;
        this.memoria = memoria;
        this.cpu1 = cpu1;
        this.cpu2 = cpu2;
        this.planificador = planificador;
        this.generador = generador;
        this.generadorInterrupciones = generadorInterrupciones;

        this.planificador.setOnSimulacionCompletada(() -> {
            SwingUtilities.invokeLater(() -> simulacionCompletada());
        });

        configurarVentana();
        inicializarComponentes();
        iniciarActualizacionUI();
    }

    @Override
    public void onEventoInterrupcion(String mensaje) {
        agregarLog(mensaje);
    }

    private void configurarVentana() {
        setTitle("UNIMET-Sat RTOS Simulator");
        setSize(1600, 950);
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

        btnIniciar = new JButton("INICIAR");
        btnIniciar.setFont(new Font("Consolas", Font.BOLD, 14));
        btnIniciar.setBackground(new Color(0, 150, 0));
        btnIniciar.setForeground(Color.WHITE);
        btnIniciar.setOpaque(true);
        btnIniciar.setBorderPainted(false);
        btnIniciar.setFocusPainted(false);
        btnIniciar.setPreferredSize(new Dimension(120, 35));

        btnPausar = new JButton("PAUSAR");
        btnPausar.setFont(new Font("Consolas", Font.BOLD, 14));
        btnPausar.setBackground(new Color(200, 150, 0));
        btnPausar.setForeground(Color.WHITE);
        btnPausar.setOpaque(true);
        btnPausar.setBorderPainted(false);
        btnPausar.setFocusPainted(false);
        btnPausar.setPreferredSize(new Dimension(120, 35));
        btnPausar.setEnabled(false);

        btnReiniciar = new JButton("REINICIAR");
        btnReiniciar.setFont(new Font("Consolas", Font.BOLD, 14));
        btnReiniciar.setBackground(new Color(180, 0, 0));
        btnReiniciar.setForeground(Color.WHITE);
        btnReiniciar.setOpaque(true);
        btnReiniciar.setBorderPainted(false);
        btnReiniciar.setFocusPainted(false);
        btnReiniciar.setPreferredSize(new Dimension(120, 35));
        btnReiniciar.setEnabled(false);

        btnIniciar.addActionListener(e -> iniciarSimulacion());
        btnPausar.addActionListener(e -> togglePausa());
        btnReiniciar.addActionListener(e -> reiniciarSimulacion());

        JPanel panelBotonesSup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBotonesSup.setOpaque(false);
        panelBotonesSup.add(btnIniciar);
        panelBotonesSup.add(btnPausar);
        panelBotonesSup.add(btnReiniciar);

        panelSuperior.add(panelIzqSup, BorderLayout.WEST);
        panelSuperior.add(panelBotonesSup, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        // ===================== PANEL CENTRAL =====================
        JPanel panelCentral = new JPanel(new BorderLayout(5, 5));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel panelCPUs = new JPanel(new GridLayout(1, 2, 8, 0));
        txtCpu1 = crearTextArea(14, true);
        txtCpu2 = crearTextArea(14, true);
        panelCPUs.add(crearScrollConTitulo(txtCpu1, "CPU 1"));
        panelCPUs.add(crearScrollConTitulo(txtCpu2, "CPU 2"));
        panelCPUs.setPreferredSize(new Dimension(0, 160));

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

        // ===================== PANEL DERECHO =====================
        JPanel panelDerecho = new JPanel();
        panelDerecho.setLayout(new BoxLayout(panelDerecho, BoxLayout.Y_AXIS));
        panelDerecho.setPreferredSize(new Dimension(340, 0));
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
            agregarLog("EMERGENCIA: " + p.getId() + " admitido directo -> " + p.getEstado());
        });
        panelAcciones.add(btnGenerar1);

        JButton btnInterrupcion = new JButton("⚡ FORZAR INTERRUPCION ⚡");
        btnInterrupcion.setFont(new Font("Consolas", Font.BOLD, 13));
        btnInterrupcion.setBackground(new Color(200, 0, 0));
        btnInterrupcion.setForeground(Color.WHITE);
        btnInterrupcion.setOpaque(true);
        btnInterrupcion.setBorderPainted(false);
        btnInterrupcion.setFocusPainted(false);
        btnInterrupcion.addActionListener(e -> {
            TipoInterrupcion[] tipos = TipoInterrupcion.values();
            TipoInterrupcion tipo = tipos[new java.util.Random().nextInt(tipos.length)];

            CPU cpuObj = new java.util.Random().nextBoolean() ? cpu1 : cpu2;
            if (cpuObj.isEnInterrupcion()) {
                cpuObj = (cpuObj == cpu1) ? cpu2 : cpu1;
            }
            if (cpuObj.isEnInterrupcion()) {
                agregarLog("[INTERRUPCION] Ambas CPUs ocupadas con ISR, reintente luego");
                return;
            }

            Interrupcion inter = new Interrupcion(
                    (int)(System.currentTimeMillis() % 10000), tipo, cpuObj, reloj, planificador, memoria);
            inter.setListener(this);
            inter.start();
            agregarLog("⚡ INTERRUPCION MANUAL: " + tipo.getDescripcion() + " en CPU-" + cpuObj.getCpuId());
        });
        panelAcciones.add(btnInterrupcion);

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
        lblInterrupciones = new JLabel("Interrupciones: 0");
        lblThroughput = new JLabel("Throughput: 0.000 p/ciclo");
        lblTiempoEsperaPromedio = new JLabel("Espera prom: 0.0 ciclos");
        lblUtilizacionCPU = new JLabel("Utilización CPU: 0.0%");

        Font fontMetrica = new Font("Consolas", Font.PLAIN, 12);
        lblProcesosEnRAM.setFont(fontMetrica);
        lblTotalProcesos.setFont(fontMetrica);
        lblTerminados.setFont(fontMetrica);
        lblDeadlineCumplidos.setFont(fontMetrica);
        lblDeadlineFallidos.setFont(fontMetrica);
        lblTasaExito.setFont(fontMetrica);
        lblTasaExito.setForeground(new Color(0, 150, 0));
        lblInterrupciones.setFont(fontMetrica);
        lblThroughput.setFont(fontMetrica);
        lblThroughput.setForeground(new Color(100, 149, 237));
        lblTiempoEsperaPromedio.setFont(fontMetrica);
        lblTiempoEsperaPromedio.setForeground(new Color(255, 165, 0));
        lblUtilizacionCPU.setFont(fontMetrica);
        lblUtilizacionCPU.setForeground(new Color(0, 200, 200));

        panelMetricas.add(lblProcesosEnRAM);
        panelMetricas.add(lblTotalProcesos);
        panelMetricas.add(lblTerminados);
        panelMetricas.add(lblDeadlineCumplidos);
        panelMetricas.add(lblDeadlineFallidos);
        panelMetricas.add(lblTasaExito);
        panelMetricas.add(lblInterrupciones);
        panelMetricas.add(lblThroughput);
        panelMetricas.add(lblTiempoEsperaPromedio);
        panelMetricas.add(lblUtilizacionCPU);
        panelDerecho.add(panelMetricas);

        // --- 9.1 Gráfico de Utilización CPU (custom Graphics2D) ---
        panelGrafico = new GraficoUtilizacionPanel();
        panelGrafico.setPreferredSize(new Dimension(320, 180));
        panelGrafico.setMinimumSize(new Dimension(320, 150));
        panelGrafico.setMaximumSize(new Dimension(400, 220));
        panelGrafico.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Gráfico CPU",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Consolas", Font.BOLD, 12)));
        panelDerecho.add(panelGrafico);

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
            generador.generarProcesosIniciales(memoria, 20);

            PoliticaPlanificacion pol = (PoliticaPlanificacion) cmbAlgoritmo.getSelectedItem();
            int quantum = (int) spnQuantum.getValue();
            planificador.cambiarAlgoritmo(pol, quantum);

            simulacionIniciada = true;
            btnIniciar.setEnabled(false);
            btnIniciar.setBackground(Color.GRAY);
            btnPausar.setEnabled(true);
            agregarLog("Simulación iniciada con " + pol.getDescripcion() + " - 20 procesos generados");
        }

        reloj.reanudar();
        planificador.reanudar();
        cpu1.reanudar();
        cpu2.reanudar();
        generadorInterrupciones.reanudar();

        lblEstadoSistema.setText("  [Ejecutando]  ");
        lblEstadoSistema.setForeground(Color.GREEN);
    }

    private void togglePausa() {
        if (planificador.isPausado()) {
            reloj.reanudar();
            planificador.reanudar();
            cpu1.reanudar();
            cpu2.reanudar();
            generadorInterrupciones.reanudar();
            btnPausar.setText("PAUSAR");
            btnPausar.setBackground(new Color(200, 150, 0));
            btnReiniciar.setEnabled(false);
            lblEstadoSistema.setText("  [Ejecutando]  ");
            lblEstadoSistema.setForeground(Color.GREEN);
            agregarLog("Simulación reanudada");
        } else {
            reloj.pausar();
            planificador.pausar();
            cpu1.pausar();
            cpu2.pausar();
            generadorInterrupciones.pausar();
            btnPausar.setText("REANUDAR");
            btnPausar.setBackground(new Color(0, 150, 0));
            btnReiniciar.setEnabled(true);
            lblEstadoSistema.setText("  [Pausado]  ");
            lblEstadoSistema.setForeground(Color.ORANGE);
            agregarLog("Simulación pausada");
        }
    }

    private void reiniciarSimulacion() {
        reloj.pausar();
        planificador.pausar();
        cpu1.pausar();
        cpu2.pausar();
        generadorInterrupciones.pausar();

        cpu1.limpiar();
        cpu2.limpiar();
        memoria.limpiar();
        reloj.reiniciar();

        // Reiniciar métricas
        planificador.getMetricas().reiniciar();
        panelGrafico.limpiar();
        ultimoIndiceGrafico = 0;
        ultimoDeadlineFallidoReportado = 0;

        simulacionIniciada = false;
        btnIniciar.setEnabled(true);
        btnIniciar.setBackground(new Color(0, 150, 0));
        btnPausar.setEnabled(false);
        btnPausar.setText("PAUSAR");
        btnPausar.setBackground(new Color(200, 150, 0));
        btnReiniciar.setEnabled(false);

        lblEstadoSistema.setText("  [Detenido]  ");
        lblEstadoSistema.setForeground(Color.ORANGE);

        txtLog.setText("");
        agregarLog("Simulación reiniciada - Presione INICIAR para comenzar");
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
            actualizarGrafico();
            detectarFallosDeadlineEnLog();
        });
        timerUI.start();
    }

    private void actualizarIndicadorSistema() {
        if (cpu1.isEnInterrupcion() || cpu2.isEnInterrupcion()) {
            lblEstadoSistema.setText("  [S.O. - ISR]  ");
            lblEstadoSistema.setForeground(Color.RED);
            return;
        }
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
        if (cpu.isEnInterrupcion()) {
            return "\n  [CPU " + numCpu + " - ATENDIENDO INTERRUPCION (ISR)]";
        }
        Proceso p = cpu.getProcesoActual();
        if (p == null) {
            return "\n  [CPU " + numCpu + " Inactiva - Esperando proceso]";
        }
        return String.format(
            "\n  %s (%s)\n" +
            "  Estado: %s | Prioridad: %d\n" +
            "  PC: %d / %d | MAR: %d\n" +
            "  Deadline: %d (rest: %d)\n" +
            "  Tipo: %s | %s\n" +
            "  E/S cada: %d ciclos | Dur E/S: %d | Quantum: %d",
            p.getNombre(), p.getId(),
            p.getEstado(), p.getPrioridad(),
            p.getPc(), p.getInstruccionesTotales(), p.getMar(),
            p.getDeadline(), p.getTiempoRestanteDeadline(),
            p.isCpuBound() ? "CPU-Bound" : "IO-Bound",
            p.isPeriodico() ? "Periódico(T=" + p.getPeriodo() + ")" : "Aperiódico",
            p.getCiclosParaES(), p.getDuracionES(),
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
            sb.append(String.format("[%s] %s | E/S: %d/%d | DL:%d\n",
                    p.getId(), p.getNombre(),
                    p.getCiclosESRestantes(), p.getDuracionES(),
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
            String dlStatus = p.getTiempoRestanteDeadline() >= 0 ? "✓" : "✗";
            sb.append(String.format("[%s] %s | Instr: %d | DL: %s\n",
                    p.getId(), p.getNombre(), p.getInstruccionesTotales(), dlStatus));
            actual = actual.getSiguiente();
        }
        if (sb.length() == 0) sb.append("(vacía)");
        return sb.toString();
    }

    private void actualizarMetricas() {
        Metricas metricas = planificador.getMetricas();
        int enRAM = memoria.getProcesosEnRAM();
        int capMax = memoria.getCapacidadMaxima();
        int terminados = memoria.getColaTerminados().getTamano();

        int cumplidos = metricas.getDeadlineCumplidos();
        int fallidos = metricas.getDeadlineFallidos();

        int totalEnSistema = memoria.getColaNuevos().getTamano()
                + memoria.getColaListos().getTamano()
                + memoria.getColaBloqueados().getTamano()
                + memoria.getColaListosSuspendidos().getTamano()
                + memoria.getColaBloqueadosSuspendidos().getTamano()
                + terminados
                + (cpu1.getProcesoActual() != null ? 1 : 0)
                + (cpu2.getProcesoActual() != null ? 1 : 0);

        double tasa = metricas.getTasaExito();
        int cicloActual = reloj.getCicloGlobal();
        double throughput = metricas.getThroughput(cicloActual);
        double esperaPromedio = metricas.getTiempoEsperaPromedio(memoria.getColaTerminados());
        double utilizacionProm = metricas.getUtilizacionPromedio();

        lblProcesosEnRAM.setText("En RAM: " + enRAM + " / " + capMax);
        lblTotalProcesos.setText("Total procesos: " + totalEnSistema);
        lblTerminados.setText("Terminados: " + terminados);
        lblDeadlineCumplidos.setText("Deadline OK: " + cumplidos);
        lblDeadlineFallidos.setText("Deadline FAIL: " + fallidos);
        lblTasaExito.setText(String.format("Tasa éxito: %.1f%%", tasa));
        lblTasaExito.setForeground(tasa >= 80 ? new Color(0, 150, 0) : Color.RED);
        lblInterrupciones.setText("Interrupciones: " + generadorInterrupciones.getContadorInterrupciones());
        lblThroughput.setText(String.format("Throughput: %.4f p/ciclo", throughput));
        lblTiempoEsperaPromedio.setText(String.format("Espera prom: %.1f ciclos", esperaPromedio));
        lblUtilizacionCPU.setText(String.format("Utilización CPU: %.1f%%", utilizacionProm));
    }

    /**
     * 9.1 — Actualizar el gráfico de utilización CPU con datos nuevos del historial.
     */
    private void actualizarGrafico() {
        Metricas metricas = planificador.getMetricas();
        int tamano = metricas.getTamanoHistorial();

        for (int i = ultimoIndiceGrafico; i < tamano; i++) {
            panelGrafico.agregarPunto(metricas.getCicloEn(i), metricas.getUtilizacionEn(i));
        }
        ultimoIndiceGrafico = tamano;

        panelGrafico.repaint();
    }

    /**
     * 9.5 — Detectar fallos de deadline y registrarlos en el log.
     */
    private void detectarFallosDeadlineEnLog() {
        Metricas metricas = planificador.getMetricas();
        int fallidosActual = metricas.getDeadlineFallidos();
        if (fallidosActual > ultimoDeadlineFallidoReportado) {
            int nuevos = fallidosActual - ultimoDeadlineFallidoReportado;
            for (int i = 0; i < nuevos; i++) {
                agregarLog("⚠ Fallo de Deadline detectado — proceso no cumplió su tiempo límite");
            }
            ultimoDeadlineFallidoReportado = fallidosActual;
        }
    }

    private void simulacionCompletada() {
        reloj.pausar();
        planificador.pausar();
        cpu1.pausar();
        cpu2.pausar();
        generadorInterrupciones.pausar();

        btnPausar.setEnabled(false);
        btnPausar.setText("PAUSAR");
        btnPausar.setBackground(new Color(200, 150, 0));
        btnReiniciar.setEnabled(true);

        lblEstadoSistema.setText("  [COMPLETADO]  ");
        lblEstadoSistema.setForeground(new Color(0, 200, 255));

        Metricas metricas = planificador.getMetricas();
        int total = metricas.getProcesosCompletados();
        int cumplidos = metricas.getDeadlineCumplidos();
        double tasa = metricas.getTasaExito();
        int cicloActual = reloj.getCicloGlobal();
        double throughput = metricas.getThroughput(cicloActual);
        double esperaProm = metricas.getTiempoEsperaPromedio(memoria.getColaTerminados());
        double utilizacion = metricas.getUtilizacionPromedio();

        agregarLog("═══════════════════════════════════════════");
        agregarLog("  SIMULACION COMPLETADA");
        agregarLog("  Total procesos: " + total);
        agregarLog("  Deadline cumplidos: " + cumplidos);
        agregarLog("  Deadline fallidos: " + (total - cumplidos));
        agregarLog(String.format("  Tasa de éxito: %.1f%%", tasa));
        agregarLog(String.format("  Throughput: %.4f procesos/ciclo", throughput));
        agregarLog(String.format("  Tiempo espera promedio: %.1f ciclos", esperaProm));
        agregarLog(String.format("  Utilización CPU promedio: %.1f%%", utilizacion));
        agregarLog("  Ciclos totales: " + cicloActual);
        agregarLog("═══════════════════════════════════════════");
    }
}
