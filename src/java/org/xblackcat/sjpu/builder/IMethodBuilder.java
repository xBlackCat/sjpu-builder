package org.xblackcat.sjpu.builder;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.lang.reflect.Method;

/**
 * 11.03.13 13:16
 *
 * @author xBlackCat
 */
public interface IMethodBuilder {
    void buildMethod(
            CtClass accessHelper,
            Class<?> targetClass,
            Method m
    ) throws NotFoundException, ReflectiveOperationException, CannotCompileException;

    boolean isAccepted(Method m);

    /**
     * Short requirement description for method to be precessed with the builder. For example "annotated with @NotNull" or "Declared in
     * interface Runnable". The method is used for generating exception text if method can't be processed.
     *
     * @return requirement to process the method with the builder.
     */
    String requirementDescription();
}
