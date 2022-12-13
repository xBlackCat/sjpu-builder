package org.xblackcat.sjpu.builder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class AProxyMethodBuilder implements IMethodBuilder {
    private final Class<?> delegateClass;

    public AProxyMethodBuilder(Class<?> delegateClass) {
        this.delegateClass = delegateClass;
    }

    public Class<?> getDelegateClass() {
        return delegateClass;
    }

    @Override
    public boolean isAccepted(Method m) {
        final Method method = BuilderUtils.findDeclaredMethod(delegateClass, m);
        return method != null && !Modifier.isAbstract(m.getModifiers());
    }

    @Override
    public String requirementDescription() {
        return "implemented in " + delegateClass.getName();
    }
}
