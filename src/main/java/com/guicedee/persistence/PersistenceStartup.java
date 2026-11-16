package com.guicedee.persistence;

import com.guicedee.client.services.lifecycle.IGuicePostStartup;
import io.smallrye.mutiny.Uni;
import java.util.List;

/** Starts the persistence services installed by Guice before application readiness completes. */
public final class PersistenceStartup implements IGuicePostStartup<PersistenceStartup> {
    @Override public List<Uni<Boolean>> postLoad() {
        return PersistenceShutdown.startRegisteredServices();
    }
    @Override public Integer sortOrder() { return 50; }
}
