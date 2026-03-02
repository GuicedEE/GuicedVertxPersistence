package com.guicedee.persistence.test.vert4;

import com.google.inject.Inject;
import io.smallrye.mutiny.Uni;
import com.guicedee.vertx.VertxEventPublisher;
import com.google.inject.name.Named;
import io.vertx.core.Vertx;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

@Log4j2
public class Verticle4Service {


    @Inject
    Mutiny.SessionFactory sessionFactory;
    @Inject
    @Named("Verticle4Bus")
    VertxEventPublisher<String> publisher;

    public Uni<Boolean> perform()
    {
        log.info("Performing work in Verticle4 on thread='{}' ctx='{}'", Thread.currentThread().getName(), Vertx.currentContext());
        return Uni.createFrom()
                .completionStage(publisher.request("go").toCompletionStage())
                .replaceWith(Boolean.TRUE);
    }
}
