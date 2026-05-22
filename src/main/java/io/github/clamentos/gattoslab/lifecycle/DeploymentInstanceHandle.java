package io.github.clamentos.gattoslab.lifecycle;

///
import io.undertow.servlet.api.InstanceHandle;

///..
import lombok.AllArgsConstructor;

///
@AllArgsConstructor

///
public final class DeploymentInstanceHandle<T> implements InstanceHandle<T> {

    ///
    private final T item;

    ///
    @Override
    public T getInstance() {

        return item;
    }

    ///..
    @Override
    public void release() {

        // No release actions.
    }

    ///
}
