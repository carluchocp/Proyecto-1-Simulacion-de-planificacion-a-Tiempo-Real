/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package estructuras;

/**
 *
 * @author diego
 */
public class Cola<T> {
    private Nodo<T> pFirst;
    private Nodo<T> pLast;
    private int size;

    public Cola() {
        this.pFirst = null;
        this.pLast = null;
        this.size = 0;
    }

    public boolean esVacia() {
        return pFirst == null;
    }

    // Enqueue (encolar)
    public void encolar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (esVacia()) {
            pFirst = nuevo;
            pLast = nuevo;
        } else {
            pLast.setSiguiente(nuevo);
            pLast = nuevo;
        }
        size++;
    }

    // Dequeue (desencolar)
    public T desencolar() {
        if (esVacia()) {
            return null;
        }
        T dato = pFirst.getContenido();
        pFirst = pFirst.getSiguiente();
        if (pFirst == null) {
            pLast = null;
        }
        size--;
        return dato;
    }

    // Peek (ver el primero sin sacarlo)
    public T proximo() {
        if (esVacia()) return null;
        return pFirst.getContenido();
    }
    
    public int getSize() {
        return size;
    }
}
