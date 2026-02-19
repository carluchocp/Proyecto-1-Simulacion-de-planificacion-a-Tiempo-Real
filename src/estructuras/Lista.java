/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package estructuras;

/**
 *
 * @author carluchocp
 */
public class Lista<T> {
    private Nodo<T> pFirst;
    private int size;

    public Lista() {
        this.pFirst = null;
        this.size = 0;
    }

    public boolean esVacia() { return pFirst == null; }

    public void insertar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (esVacia()) { pFirst = nuevo; } 
        else {
            Nodo<T> aux = pFirst;
            while (aux.getSiguiente() != null) aux = aux.getSiguiente();
            aux.setSiguiente(nuevo);
        }
        size++;
    }

    public void eliminar(T dato) {
        if (esVacia()) return;
        if (pFirst.getContenido() == dato) {
            pFirst = pFirst.getSiguiente();
            size--;
            return;
        }
        Nodo<T> aux = pFirst;
        while (aux.getSiguiente() != null) {
            if (aux.getSiguiente().getContenido() == dato) {
                aux.setSiguiente(aux.getSiguiente().getSiguiente());
                size--;
                return;
            }
            aux = aux.getSiguiente();
        }
    }

    // --- NUEVOS MÉTODOS PARA EL SWAP ---
    public T getUltimo() {
        if (esVacia()) return null;
        Nodo<T> aux = pFirst;
        while (aux.getSiguiente() != null) aux = aux.getSiguiente();
        return aux.getContenido();
    }

    public int getSize() { return size; }
    public Nodo<T> getpFirst() { return pFirst; }
}
