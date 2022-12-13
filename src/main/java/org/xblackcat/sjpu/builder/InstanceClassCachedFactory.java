package org.xblackcat.sjpu.builder;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InstanceClassCachedFactory<Base> extends ACachedFactory<Base, Class<? extends Base>> {
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    public InstanceClassCachedFactory(IBuilder<Base> builder, Class<?>... argClasses) {
        super(builder, argClasses);
    }

    public <T extends Base> T get(Class<T> clazz, Object... args) throws GeneratorException {
        Key key = new Key(clazz, args);

        Class<? extends Base> accessHelperClass;

        lock.readLock().lock();
        try {
            accessHelperClass = helpers.get(key);

        } finally {
            lock.readLock().unlock();
        }

        if (accessHelperClass == null) {
            lock.writeLock().lock();
            try {
                final Class<? extends T> builtClass = builder.build(clazz);
                Class<? extends Base> oldAccessHelper = helpers.get(key);

                if (oldAccessHelper != null) {
                    accessHelperClass = oldAccessHelper;
                } else {
                    accessHelperClass = builtClass;
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        @SuppressWarnings("unchecked")
        final Class<? extends T> helperClass = (Class<? extends T>) accessHelperClass;
        return instantiate(helperClass, args);
    }
}
