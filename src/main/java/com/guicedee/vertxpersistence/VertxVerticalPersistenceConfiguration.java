package com.guicedee.vertxpersistence;
import com.guicedee.vertx.spi.*;
import io.vertx.core.VertxBuilder;

public class VertxVerticalPersistenceConfiguration implements VertxConfigurator
{
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
