/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author carluchocp
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CargadorArchivos {

    /**
     * Carga procesos desde un archivo .csv o .json y los admite en memoria.
     * @return cantidad de procesos cargados, o -1 si hubo error.
     */
    public static int cargarDesdeArchivo(File archivo, Memoria memoria) {
        String nombre = archivo.getName().toLowerCase();
        if (nombre.endsWith(".csv")) {
            return cargarCSV(archivo, memoria);
        } else if (nombre.endsWith(".json")) {
            return cargarJSON(archivo, memoria);
        }
        return -1;
    }

    // ======================== CSV ========================
    // Formato esperado (con encabezado):
    // nombre,instrucciones,prioridad,deadline,cpuBound,periodico,periodo,ciclosParaES,duracionES
    private static int cargarCSV(File archivo, Memoria memoria) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea = br.readLine(); // saltar encabezado
            if (linea == null) return 0;

            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                try {
                    Proceso p = parsearLineaCSV(linea);
                    if (p != null) {
                        memoria.admitirProceso(p);
                        count++;
                    }
                } catch (Exception e) {
                    // Línea inválida, se ignora
                }
            }
        } catch (IOException e) {
            return -1;
        }
        return count;
    }

    private static Proceso parsearLineaCSV(String linea) {
        String[] partes = linea.split(",");
        if (partes.length < 9) return null;

        String nombre = partes[0].trim();
        int instrucciones = Integer.parseInt(partes[1].trim());
        int prioridad = Integer.parseInt(partes[2].trim());
        int deadline = Integer.parseInt(partes[3].trim());
        boolean cpuBound = Boolean.parseBoolean(partes[4].trim());
        boolean periodico = Boolean.parseBoolean(partes[5].trim());
        int periodo = Integer.parseInt(partes[6].trim());
        int ciclosParaES = Integer.parseInt(partes[7].trim());
        int duracionES = Integer.parseInt(partes[8].trim());

        return new Proceso(nombre, instrucciones, prioridad, deadline,
                cpuBound, periodico, periodo, ciclosParaES, duracionES);
    }

    // ======================== JSON ========================
    // Formato esperado: array de objetos, parseo manual (sin librerías externas de estructura)
    // [
    //   { "nombre":"P1", "instrucciones":10, "prioridad":3, "deadline":50,
    //     "cpuBound":true, "periodico":false, "periodo":0, "ciclosParaES":5, "duracionES":2 },
    //   ...
    // ]
    private static int cargarJSON(File archivo, Memoria memoria) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea.trim());
            }
            String contenido = sb.toString();

            // Quitar corchetes exteriores
            contenido = contenido.trim();
            if (contenido.startsWith("[")) contenido = contenido.substring(1);
            if (contenido.endsWith("]")) contenido = contenido.substring(0, contenido.length() - 1);

            // Dividir por objetos: buscar pares { }
            int i = 0;
            while (i < contenido.length()) {
                int inicio = contenido.indexOf('{', i);
                if (inicio == -1) break;
                int fin = contenido.indexOf('}', inicio);
                if (fin == -1) break;

                String obj = contenido.substring(inicio + 1, fin);
                try {
                    Proceso p = parsearObjetoJSON(obj);
                    if (p != null) {
                        memoria.admitirProceso(p);
                        count++;
                    }
                } catch (Exception e) {
                    // Objeto inválido, se ignora
                }
                i = fin + 1;
            }
        } catch (IOException e) {
            return -1;
        }
        return count;
    }

    private static Proceso parsearObjetoJSON(String obj) {
        String nombre = extraerString(obj, "nombre");
        int instrucciones = extraerInt(obj, "instrucciones");
        int prioridad = extraerInt(obj, "prioridad");
        int deadline = extraerInt(obj, "deadline");
        boolean cpuBound = extraerBoolean(obj, "cpuBound");
        boolean periodico = extraerBoolean(obj, "periodico");
        int periodo = extraerInt(obj, "periodo");
        int ciclosParaES = extraerInt(obj, "ciclosParaES");
        int duracionES = extraerInt(obj, "duracionES");

        if (nombre == null || instrucciones <= 0) return null;

        return new Proceso(nombre, instrucciones, prioridad, deadline,
                cpuBound, periodico, periodo, ciclosParaES, duracionES);
    }

    private static String extraerString(String obj, String clave) {
        int idx = obj.indexOf("\"" + clave + "\"");
        if (idx == -1) return null;
        int dosP = obj.indexOf(':', idx);
        if (dosP == -1) return null;
        String rest = obj.substring(dosP + 1).trim();
        if (rest.startsWith("\"")) {
            int cierre = rest.indexOf('"', 1);
            if (cierre == -1) return null;
            return rest.substring(1, cierre);
        }
        return null;
    }

    private static int extraerInt(String obj, String clave) {
        int idx = obj.indexOf("\"" + clave + "\"");
        if (idx == -1) return 0;
        int dosP = obj.indexOf(':', idx);
        if (dosP == -1) return 0;
        String rest = obj.substring(dosP + 1).trim();
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (Character.isDigit(c) || c == '-') num.append(c);
            else if (num.length() > 0) break;
        }
        if (num.length() == 0) return 0;
        return Integer.parseInt(num.toString());
    }

    private static boolean extraerBoolean(String obj, String clave) {
        int idx = obj.indexOf("\"" + clave + "\"");
        if (idx == -1) return false;
        int dosP = obj.indexOf(':', idx);
        if (dosP == -1) return false;
        String rest = obj.substring(dosP + 1).trim().toLowerCase();
        return rest.startsWith("true");
    }
}
