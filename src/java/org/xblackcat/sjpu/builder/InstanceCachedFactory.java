package org.xblackcat.sjpu.builder;

/**
 * 04.04.2014 15:47
 *
 * @author xBlackCat
 */
public class InstanceCachedFactory<Base> extends ACachedFactory<Base, Base> {
    public InstanceCachedFactory(IBuilder<Base> builder, Class<?>... argClasses) {
        super(builder, argClasses);
    }

    public <T extends Base> T get(Class<T> clazz, Object... args) throws GeneratorException {
        Key key = new Key(clazz, args);

        lock.readLock().lock();
        try {
            @SuppressWarnings({"unchecked"})
            T accessHelper = (T) helpers.get(key);

            if (accessHelper != null) {
                return accessHelper;
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            final Class<? extends T> builtClass = builder.build(clazz);
            T accessHelper = instantiate(builtClass, args);
            @SuppressWarnings({"unchecked"})
            T oldAccessHelper = (T) helpers.get(key);

            if (oldAccessHelper != null) {
                return oldAccessHelper;
            }

            helpers.put(key, accessHelper);

            return accessHelper;
        } finally {
            lock.writeLock().unlock();
        }
    }

}
