package com.guicedee.vertxpersistence;
import com.guicedee.vertx.spi.*;
import io.vertx.core.VertxBuilder;

/**
 * Vert.x configuration hook used by the persistence module.
 * Keeps the builder available for future customizations without forcing defaults.
 */
public class VertxVerticalPersistenceConfiguration implements VertxConfigurator
{
    /**
     * Returns the provided builder after optional customization.
     *
     * @param builder the Vert.x builder passed in by the runtime
     * @return the same builder instance for chaining
     */
    @Override
    public VertxBuilder builder(VertxBuilder builder)
    {
        /*builder.withMetrics()
                .withTracer()
                .withTransport()
                .withClusterManager()*/
        return builder;
    }
}
