package org.xblackcat.sjpu.builder;

import javassist.*;

import java.lang.reflect.Method;

public interface IDefiner {
    CtClass getBaseCtClass() throws NotFoundException;

    boolean isAssignable(Class<?> clazz);

    boolean isImplemented(Method m);

    String getNestedClassName();

    CtConstructor buildCtConstructor(CtClass accessHelper) throws NotFoundException, CannotCompileException;

    ClassPool getPool();
}
