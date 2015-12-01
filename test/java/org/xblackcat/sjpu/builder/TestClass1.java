package org.xblackcat.sjpu.builder;

import java.util.concurrent.Callable;

/**
 * 20.10.2015 20:17
 *
 * @author xBlackCat
 */
public class TestClass1<A, B> implements Callable<A> {
    @Override
    public A call() throws Exception {
        return null;
    }

    public B method() {
        return null;
    }
}
