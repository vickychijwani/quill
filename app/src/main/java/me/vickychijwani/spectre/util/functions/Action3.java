package me.vickychijwani.spectre.util.functions;

/**
 * A three-argument action.
 */
public interface Action3<T1, T2, T3> {

    void call(T1 t1, T2 t2, T3 t3);

}
