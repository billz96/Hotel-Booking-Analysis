package com.bill.zografos;

/**
 * Created by vasilis on 07/11/2020.
 */
public interface Handler<T> {
    void run(T arg);
}
