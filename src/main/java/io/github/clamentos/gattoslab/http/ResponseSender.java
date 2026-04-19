package io.github.clamentos.gattoslab.http;

///
@FunctionalInterface

///
public interface ResponseSender {

    ///
    @SuppressWarnings("squid:S112")
    void send() throws Exception;

    ///
}
