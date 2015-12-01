package org.xblackcat.sjpu.builder;

/**
 * 04.04.2014 15:40
 *
 * @author xBlackCat
 */
public interface IBuilder<Base> {
    <T extends Base> Class<? extends T> build(Class<T> target) throws GeneratorException;
}
