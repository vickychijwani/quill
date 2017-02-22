package me.vickychijwani.spectre.util.functions;

/**
 * A two-argument action.
 */
public interface Action2<T1, T2> {

    void call(T1 t1, T2 t2);

}
