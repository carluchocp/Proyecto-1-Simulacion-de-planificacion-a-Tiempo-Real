/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package estructuras;

/**
 *
 * @author carluchocp
 */
public class Cola<T> {

    private Nodo<T> primero;
    private Nodo<T> ultimo;
    private int tamano;

    public Cola() {
        this.primero = null;
        this.ultimo = null;
        this.tamano = 0;
    }

    public void encolar(T contenido) {
        Nodo<T> nuevo = new Nodo<>(contenido);
        if (ultimo == null) {
            primero = nuevo;
            ultimo = nuevo;
        } else {
            ultimo.setSiguiente(nuevo);
            ultimo = nuevo;
        }
        tamano++;
    }

    public T desencolar() {
        if (primero == null) {
            return null;
        }
        T contenido = primero.getContenido();
        primero = primero.getSiguiente();
        if (primero == null) {
            ultimo = null;
        }
        tamano--;
        return contenido;
    }

    public boolean estaVacia() {
        return primero == null;
    }

    public Nodo<T> getPrimerNodo() {
        return primero;
    }

    public int getTamano() {
        return tamano;
    }
}
