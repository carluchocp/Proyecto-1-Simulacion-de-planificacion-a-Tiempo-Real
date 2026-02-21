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

    public static int cargarDesdeArchivo(File archivo, Memoria memoria) {
        if (archivo == null || !archivo.exists()) {
            System.err.println("Error: El archivo no existe o es nulo.");
            return 0;
        }

        String nombre = archivo.getName().toLowerCase();
        if (nombre.endsWith(".csv")) {
            return cargarCSV(archivo, memoria);
        } else if (nombre.endsWith(".json")) {
            return cargarJSON(archivo, memoria);
        }
        
        System.err.println("Formato no soportado. Debe ser .csv o .json");
        return 0; 
    }

    // ======================== Análisis de CSV ========================
    // Formato esperado:
    // nombre,instrucciones,prioridad,deadline,cpuBound,periodico,periodo,ciclosParaES,duracionES
    private static int cargarCSV(File archivo, Memoria memoria) {
        int procesosCargados = 0;
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea = br.readLine(); // Saltar la primera línea (encabezado)
            
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue; // Ignorar líneas en blanco

                String[] partes = linea.split(",");
                if (partes.length < 9) {
                    System.err.println("Línea ignorada por formato incompleto: " + linea);
                    continue; 
                }

                try {
                    // Aquí deberías instanciar tu Proceso usando los datos de 'partes'
                    // Ejemplo asumiendo tu modelo:
                    // String nombre = partes[0].trim();
                    // int instrucciones = Integer.parseInt(partes[1].trim());
                    // int prioridad = Integer.parseInt(partes[2].trim());
                    // int deadline = Integer.parseInt(partes[3].trim());
                    // boolean cpuBound = Boolean.parseBoolean(partes[4].trim());
                    // boolean periodico = Boolean.parseBoolean(partes[5].trim());
                    // int periodo = Integer.parseInt(partes[6].trim());
                    // int ciclosParaES = Integer.parseInt(partes[7].trim());
                    // int duracionES = Integer.parseInt(partes[8].trim());

                    /* * TODO: Crea el objeto Proceso aquí con los datos extraídos
                     * Proceso nuevoProceso = new Proceso(...);
                     * memoria.encolarNuevo(nuevoProceso);
                     */
                     
                    procesosCargados++;
                } catch (NumberFormatException e) {
                    System.err.println("Error leyendo números en la línea: " + linea);
                }
            }
        } catch (IOException e) {
            System.err.println("Error de lectura del archivo CSV: " + e.getMessage());
        }
        
        return procesosCargados;
    }

    // ======================== Análisis de JSON ========================
    private static int cargarJSON(File archivo, Memoria memoria) {
        int procesosCargados = 0;
        
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = br.readLine()) != null) {
                sb.append(linea.trim());
            }
            
            String contenidoJson = sb.toString();
            // TODO: Lógica para separar los bloques { } del JSON e instanciar los procesos
            // Puedes usar tus métodos extraerString, extraerInt aquí, pasándole cada bloque.
            
        } catch (IOException e) {
             System.err.println("Error de lectura del archivo JSON: " + e.getMessage());
        }
        
        return procesosCargados;
    }

    // ======================== Utilidades Extracción Manual JSON ========================

    public static String extraerString(String obj, String clave) {
        try {
            int idx = obj.indexOf("\"" + clave + "\"");
            if (idx == -1) return null;
            
            int dosP = obj.indexOf(':', idx);
            if (dosP == -1) return null;
            
            String rest = obj.substring(dosP + 1).trim();
            if (rest.startsWith("\"")) {
                int cierre = rest.indexOf('\"', 1);
                if (cierre == -1) return null;
                return rest.substring(1, cierre);
            }
        } catch (Exception e) {
            System.err.println("Error extrayendo String para la clave: " + clave);
        }
        return null;
    }

    public static int extraerInt(String obj, String clave) {
        try {
            int idx = obj.indexOf("\"" + clave + "\"");
            if (idx == -1) return 0;
            
            int dosP = obj.indexOf(':', idx);
            if (dosP == -1) return 0;
            
            String rest = obj.substring(dosP + 1).trim();
            StringBuilder num = new StringBuilder();
            
            for (int i = 0; i < rest.length(); i++) {
                char c = rest.charAt(i);
                if (Character.isDigit(c) || c == '-') {
                    num.append(c);
                } else if (num.length() > 0 && c != ' ' && c != ',') {
                    // Romper solo si ya tenemos números y el caracter no es un espacio o coma permitida
                    break;
                }
            }
            
            if (num.length() == 0) return 0;
            return Integer.parseInt(num.toString());
            
        } catch (Exception e) {
            System.err.println("Error extrayendo Int para la clave: " + clave);
            return 0;
        }
    }

    public static boolean extraerBoolean(String obj, String clave) {
        try {
            int idx = obj.indexOf("\"" + clave + "\"");
            if (idx == -1) return false;
            
            int dosP = obj.indexOf(':', idx);
            if (dosP == -1) return false;
            
            String rest = obj.substring(dosP + 1).trim().toLowerCase();
            return rest.startsWith("true");
        } catch (Exception e) {
            System.err.println("Error extrayendo Boolean para la clave: " + clave);
            return false;
        }
    }
}