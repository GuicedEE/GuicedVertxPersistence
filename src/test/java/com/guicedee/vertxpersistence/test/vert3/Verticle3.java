package com.guicedee.vertxpersistence.test.vert3;

import com.guicedee.vertx.spi.Verticle;
import com.guicedee.vertx.VertxEventDefinition;
import com.guicedee.client.IGuiceContext;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import lombok.extern.log4j.Log4j2;

@Verticle(workerPoolName = "Verticle3")
@Log4j2
public class Verticle3 {
    @VertxEventDefinition("Verticle3Bus")
    public Uni<Boolean> onEvent(String payload) {
        Mutiny.SessionFactory sf = IGuiceContext.get(Mutiny.SessionFactory.class);
        return sf.openSession()
                .chain(s -> s.withTransaction(tx -> s.createNativeQuery("select pg_sleep(1)").getSingleResult())
                        .eventually(s::close))
                .replaceWith(Boolean.TRUE);
    }
}
