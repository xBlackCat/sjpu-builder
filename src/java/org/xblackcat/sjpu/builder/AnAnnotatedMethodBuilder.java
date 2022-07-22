package org.xblackcat.sjpu.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class AnAnnotatedMethodBuilder<A extends Annotation> implements IMethodBuilder {
    protected final Log log = LogFactory.getLog(getClass());
    protected final Class<A> annClass;

    public AnAnnotatedMethodBuilder(Class<A> annClass) {
        this.annClass = annClass;
    }

    public Class<A> getAnnotationClass() {
        return annClass;
    }

    @Override
    public boolean isAccepted(Method m) {
        return m.isAnnotationPresent(getAnnotationClass());
    }

    @Override
    public String requirementDescription() {
        return "annotated with " + getAnnotationClass().getName();
    }

    protected A getAnnotation(Method m) {
        return m.getAnnotation(getAnnotationClass());
    }
}
