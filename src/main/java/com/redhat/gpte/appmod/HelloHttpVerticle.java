package com.redhat.gpte.appmod;

import io.jaegertracing.Configuration;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HelloHttpVerticle extends AbstractVerticle {
    private final Tracer tracer;

    public HelloHttpVerticle() {
        String serviceName = System.getenv("JAEGER_SERVICE_NAME");
        if (null == serviceName || serviceName.isEmpty()) {
            // outside of openshift, this is the service name
            // inside openshift, it should be the artifact name -- see the deployment.yml file
            serviceName = "HelloHttpVerticle";
        }

        tracer = Configuration.fromEnv(serviceName).getTracer();
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);

        router.get("/").handler(this::hello);
        router.get("/:name").handler(this::hello);

        vertx.createHttpServer()
            .requestHandler(router::accept)
            .listen(8080);
    }

    private void hello(RoutingContext rc) {
        try (Scope ignored = tracer.buildSpan(rc.currentRoute().getPath()).startActive(true)) {
            String message = "Hello";

            if (rc.pathParam("name") != null) {
                message += " " + rc.pathParam("name");
            }

            JsonObject json = new JsonObject()
                .put("message", message)
                .put("served-by", System.getenv("HOSTNAME"));

            rc.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(json.encode());
        }
    }
}
