package com.guicedee.vertxpersistence.test;

import com.guicedee.vertxpersistence.implementations.postgres.PostgresConnectionBaseInfo;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
//import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import jakarta.persistence.Persistence;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.Map;

import static com.guicedee.vertxpersistence.test.PostgresTest.*;
import static com.guicedee.vertxpersistence.test.PostgresTest.POSTGRES_PASSWORD;
import static com.guicedee.vertxpersistence.test.PostgresTest.POSTGRES_USER;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbVerticle extends AbstractVerticle
{
    private static final Logger logger = LoggerFactory.getLogger(DbVerticle.class);
    private Mutiny.SessionFactory emf;  // <1>

    @Override
    public Uni<Void> asyncStart() {
// end::preamble[]

        // tag::hr-start[]
        PostgresConnectionBaseInfo connectionInfo = new PostgresConnectionBaseInfo();
        connectionInfo.setServerName(POSTGRES_HOST);
        connectionInfo.setPort(String.valueOf(POSTGRES_PORT));
        connectionInfo.setDatabaseName(POSTGRES_DATABASE);
        connectionInfo.setUsername(POSTGRES_USER);
        connectionInfo.setPassword(POSTGRES_PASSWORD);
        connectionInfo.setDefaultConnection(true);
        connectionInfo.setReactive(true);

        var url = connectionInfo.getJdbcUrl();


        Uni<Void> startHibernate = Uni.createFrom().deferred(() -> {
            var props = Map.of("javax.persistence.jdbc.url", url,
                    "javax.persistence.jdbc.user",POSTGRES_USER,
                    "javax.persistence.jdbc.password",POSTGRES_PASSWORD
            );  // <1>

            emf = Persistence
                    .createEntityManagerFactory("testPostgresReactive", props)
                    .unwrap(Mutiny.SessionFactory.class);

            return Uni.createFrom().voidItem();
        });

        startHibernate = vertx.executeBlocking(startHibernate)  // <2>
                .onItem().invoke(() -> logger.info("✅ Hibernate Reactive is ready"));
        // end::hr-start[]

        // tag::routing[]
        Router router = Router.router(vertx);

        BodyHandler bodyHandler = BodyHandler.create();
        router.post().handler(bodyHandler::handle);

      /*  router.get("/products").respond(this::listProducts);
        router.get("/products/:id").respond(this::getProduct);
        router.post("/products").respond(this::createProduct);*/
        // end::routing[]

        // tag::async-start[]
        Uni<HttpServer> startHttpServer = vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080)
                .onItem().invoke(() -> logger.info("✅ HTTP server listening on port 8080"));

        return Uni.combine().all().unis(startHibernate, startHttpServer).discardItems();  // <1>
        // end::async-start[]
    }

    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();

        logger.info("🚀 Starting a PostgreSQL container");

        logger.info("🚀 Starting Vert.x");

        // tag::vertx-start[]
        Vertx vertx = Vertx.vertx();

        DeploymentOptions options = new DeploymentOptions(); // <1>
        long tcTime = System.currentTimeMillis();
        vertx.deployVerticle(DbVerticle::new, options).subscribe().with(  // <2>
                ok -> {
                    long vertxTime = System.currentTimeMillis();
                    logger.info("✅ Deployment success");
                    logger.info("💡 PostgreSQL container started in {}ms", (tcTime - startTime));
                    logger.info("💡 Vert.x app started in {}ms", (vertxTime - tcTime));
                },
                err -> logger.error("🔥 Deployment failure", err));
        // end::vertx-start[]
    }
}
