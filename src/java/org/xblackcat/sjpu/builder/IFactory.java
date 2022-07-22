package org.xblackcat.sjpu.builder;

import java.util.concurrent.locks.ReadWriteLock;

public interface IFactory<T> {
    <I extends T> I get(Class<I> clazz, Object... args);

    ReadWriteLock getLock();

    void purge();
}
