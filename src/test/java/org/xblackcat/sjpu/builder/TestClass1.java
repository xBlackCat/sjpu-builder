package org.xblackcat.sjpu.builder;

import java.util.concurrent.Callable;

public class TestClass1<A, B> implements Callable<A> {
    @Override
    public A call() throws Exception {
        return null;
    }

    public B method() {
        return null;
    }
}
