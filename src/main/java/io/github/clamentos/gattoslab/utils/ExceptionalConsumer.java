package io.github.clamentos.gattoslab.utils;

///
@FunctionalInterface

///
public interface ExceptionalConsumer<A, B> {

    ///
    @SuppressWarnings("squid:S112")
    public void execute(final A a, final B b) throws Exception;

    ///
}
