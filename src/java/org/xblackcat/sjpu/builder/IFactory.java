package org.xblackcat.sjpu.builder;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * 04.04.2014 13:09
 *
 * @author xBlackCat
 */
public interface IFactory<T> {
    <I extends T> I get(Class<I> clazz, Object... args);

    ReadWriteLock getLock();

    void purge();
}
