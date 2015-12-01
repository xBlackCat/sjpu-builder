package org.xblackcat.sjpu.builder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Functional builder allows only one abstract method per class/interface.
 *
 * @author xBlackCat
 */
public class FunctionalClassBuilder<Base> extends ClassBuilder<Base> {
    public FunctionalClassBuilder(IDefiner definerF, IMethodBuilder... builders) {
        super(definerF, builders);
    }

    @Override
    public <T extends Base> Class<? extends T> build(
            Class<T> target
    ) throws GeneratorException {
        int abstractCount = 0;
        for (Method m : target.getMethods()) {
            if (Modifier.isAbstract(m.getModifiers())) {
                abstractCount++;
            }
        }
        for (Method m : target.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) && Modifier.isAbstract(m.getModifiers())) {
                abstractCount++;
            }
        }

        if (abstractCount != 1) {
            throw new GeneratorException(
                    "Only single abstract method is allowed for implementation of functional interface/class. "
                            + BuilderUtils.getName(target) + " has " + abstractCount
            );
        }

        return super.build(target);
    }
}
