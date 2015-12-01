package org.xblackcat.sjpu.builder;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 14.10.2015 14:48
 *
 * @author xBlackCat
 */
public abstract class ACachedFactory<Base, CacheObj> implements IFactory<Base> {
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected final Class<?>[] argClasses;
    protected final Map<Key, CacheObj> helpers = new HashMap<>();
    protected final IBuilder<Base> builder;

    public ACachedFactory(IBuilder<Base> builder, Class<?>... argClasses) {
        this.argClasses = argClasses;
        this.builder = builder;
    }

    protected <T extends Base> T instantiate(Class<? extends T> builtClass, Object[] args) throws GeneratorException {
        try {
            return builtClass.getConstructor(argClasses).newInstance(args);
        } catch (InstantiationException e) {
            throw new GeneratorException("Class is not implemented", e);
        } catch (IllegalAccessException e) {
            throw new GeneratorException("Access helper constructor should be public", e);
        } catch (InvocationTargetException e) {
            throw new GeneratorException("Exception occurs in access helper constructor", e);
        } catch (NoSuchMethodException e) {
            throw new GeneratorException(
                    "Access helper class constructor should have the following signature: " +
                            builtClass.getName() + "(" + ArrayUtils.toString(argClasses) + " arg);",
                    e
            );
        }
    }

    @Override
    public ReadWriteLock getLock() {
        return lock;
    }

    @Override
    public void purge() {
        lock.writeLock().lock();
        try {
            helpers.clear();
        } finally {
            lock.writeLock().unlock();
        }

    }

    protected final static class Key {
        private final Class<?> clazz;
        private final Object[] args;

        public Key(Class<?> clazz, Object[] args) {
            this.clazz = clazz;
            this.args = args;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return Objects.equals(clazz, key.clazz) &&
                    Arrays.equals(args, key.args);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, Arrays.hashCode(args));
        }
    }
}
