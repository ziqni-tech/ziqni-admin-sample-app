package com.ziqni.admin.collections;

public class Tuple<T1, T2> {

    public final T1 one;
    public final T2 two;


    public Tuple(T1 one, T2 two) {
        this.one = one;
        this.two = two;
    }

    public static <TOne, TTwo> Tuple<TOne, TTwo> as(TOne one, TTwo two){
        return new Tuple<TOne, TTwo>(one,two);
    }
}
