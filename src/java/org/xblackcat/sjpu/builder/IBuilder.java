package org.xblackcat.sjpu.builder;

public interface IBuilder<Base> {
    <T extends Base> Class<? extends T> build(Class<T> target) throws GeneratorException;
}
