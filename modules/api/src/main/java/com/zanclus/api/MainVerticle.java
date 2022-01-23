package com.zanclus.api;

import com.zanclus.LocalDateCodec;
import com.zanclus.api.services.*;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.api.service.RouteToEBServiceHandler;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.openapi.RouterBuilder;
import io.vertx.serviceproxy.ServiceBinder;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.persistence.Persistence;
import java.time.LocalDate;

/**
 * Main Vert.x Verticle, entrypoint for this application
 */
public class MainVerticle extends AbstractVerticle {
    
    static Uni<Mutiny.SessionFactory> initHibernate() {
        return Uni.createFrom().item(Persistence.createEntityManagerFactory("dev")
            .unwrap(Mutiny.SessionFactory.class));
    }
    
    Uni<RouterBuilder> initOpenAPI(Mutiny.SessionFactory sessionFactory) {
        SystemApiImpl sysApi = new SystemApiImpl(sessionFactory);
    
        ServiceBinder sysSvcBinder = new ServiceBinder(vertx.getDelegate());
        sysSvcBinder.setAddress("budjet.system").register(SystemApi.class, sysApi);
    
        IncomeApiImpl incomeApi = new IncomeApiImpl(sessionFactory);
        ServiceBinder incomeBinder = new ServiceBinder(vertx.getDelegate());
        incomeBinder.setAddress("budjet.income").register(IncomeApi.class, incomeApi);
    
        BillsApiImpl billsApi = new BillsApiImpl(sessionFactory);
        ServiceBinder billsBinder = new ServiceBinder(vertx.getDelegate());
        billsBinder.setAddress("budjet.bills").register(BillsApi.class, billsApi);
        
        return RouterBuilder.create(vertx, "openapi.yml");
    }
    
    Router mapRoutes(RouterBuilder routerBuilder) {
        
        
        
        // Iterate over all operations and map their operationId and `x-vertx-event-bus` addresses to the routes
        routerBuilder.operations().forEach(operation -> {
            String mappedEventBusAddress = operation.getOperationModel().getString("x-vertx-event-bus");
            String operationId = operation.getOperationId();
            
            routerBuilder
                .operation(operationId)
                .getDelegate()
                .handler(
                    RouteToEBServiceHandler.build(vertx.eventBus().getDelegate(), mappedEventBusAddress, operationId)
                );
        });
        
        return routerBuilder.createRouter();
    }
    
    Uni<HttpServer> createServer(Router router) {
        return vertx.createHttpServer().requestHandler(router).listen(8080);
    }
    
    @Override
    public Uni<Void> asyncStart() {
        vertx.eventBus().getDelegate().registerDefaultCodec(LocalDate.class, new LocalDateCodec());
        
        Uni<Mutiny.SessionFactory> startHibernate = Uni.createFrom().deferred(MainVerticle::initHibernate);

        return vertx.executeBlocking(startHibernate)
                    .flatMap(this::initOpenAPI)
                    .map(this::mapRoutes)
                    .flatMap(this::createServer)
                    .replaceWithVoid();
    }
}
