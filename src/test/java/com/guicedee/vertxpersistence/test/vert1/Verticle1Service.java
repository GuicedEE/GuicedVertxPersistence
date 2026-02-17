package com.guicedee.vertxpersistence.test.vert1;

import com.google.inject.Inject;
import io.smallrye.mutiny.Uni;
import com.guicedee.vertx.VertxEventPublisher;
import com.google.inject.name.Named;
import io.vertx.core.Vertx;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

@Log4j2
public class Verticle1Service {


    @Inject
    Mutiny.SessionFactory sessionFactory;
    @Inject
    @Named("Verticle1Bus")
    VertxEventPublisher<String> publisher;

    public Uni<Boolean> perform() {
        log.info("Performing work in Verticle1 on thread='{}' ctx='{}'", Thread.currentThread().getName(), Vertx.currentContext());
        // Request/reply so the test can await completion of the DB work handled by the consumer method
        return Uni.createFrom()
                .completionStage(publisher.request("go").toCompletionStage())
                .replaceWith(Boolean.TRUE);
    }


}
