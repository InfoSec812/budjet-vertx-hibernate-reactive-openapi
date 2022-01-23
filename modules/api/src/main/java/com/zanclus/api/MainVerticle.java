package com.zanclus.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zanclus.api.services.*;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.openapi.RouterBuilder;
import io.vertx.serviceproxy.ServiceBinder;
import org.hibernate.reactive.mutiny.Mutiny;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Persistence;

/**
 * Main Vert.x Verticle, entrypoint for this application
 */
public class MainVerticle extends AbstractVerticle {
    
    private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);
    
    /**
     * Initialize Hibernate/JPA from the `persistence.xml`
     * @return A {@link Uni} which will resolve to a {@link Mutiny.SessionFactory} on success
     */
    static Uni<Mutiny.SessionFactory> initHibernate() {
        return Uni.createFrom().item(Persistence.createEntityManagerFactory("dev")
            .unwrap(Mutiny.SessionFactory.class));
    }
    
    /**
     * Initialize and bind the WebApiService instances and the load the OpenAPI contract
     * @param sessionFactory The {@link Mutiny.SessionFactory} used in the WebApiService
     *                       instances for interacting with the database
     * @return A {@link Uni} which will resolve to a {@link RouterBuilder} on success
     */
    Uni<RouterBuilder> initOpenAPI(Mutiny.SessionFactory sessionFactory) {
        SystemApiImpl sysApi = (SystemApiImpl)SystemApi.create(vertx.getDelegate());
        sysApi.setSessionFactory(sessionFactory);
        ServiceBinder sysSvcBinder = new ServiceBinder(vertx.getDelegate());
        sysSvcBinder.setAddress("budjet.system").register(SystemApi.class, sysApi);
    
        IncomeApiImpl incomeApi = (IncomeApiImpl)IncomeApi.create(vertx.getDelegate());
        incomeApi.setSessionFactory(sessionFactory);
        ServiceBinder incomeBinder = new ServiceBinder(vertx.getDelegate());
        incomeBinder.setAddress("budjet.income").register(IncomeApi.class, incomeApi);
    
        BillsApiImpl billsApi = (BillsApiImpl)BillsApi.create(vertx.getDelegate());
        billsApi.setSessionFactory(sessionFactory);
        ServiceBinder billsBinder = new ServiceBinder(vertx.getDelegate());
        billsBinder.setAddress("budjet.bills").register(BillsApi.class, billsApi);
    
        CashflowApiImpl cashflowApi = (CashflowApiImpl)CashflowApi.create(vertx.getDelegate());
        cashflowApi.setSessionFactory(sessionFactory);
        ServiceBinder cashflowBinder = new ServiceBinder(vertx.getDelegate());
        cashflowBinder.setAddress("budjet.cashflow").register(CashflowApi.class, cashflowApi);
        
        return RouterBuilder.create(vertx, "openapi.yml");
    }
    
    /**
     * Mount the WebApiServices to the OpenAPI RouterBuilder
     * @param routerBuilder The {@link RouterBuilder} created from the OpenAPI contract
     * @return a {@link Uni} which resolves to a {@link Router} on success
     */
    Uni<Router> mapRoutes(RouterBuilder routerBuilder) {
        RouterBuilderOptions routerOptions = new RouterBuilderOptions()
                                                 .setMountNotImplementedHandler(true);
        routerBuilder.setOptions(routerOptions);
        
        routerBuilder.mountServicesFromExtensions();
        
        return Uni.createFrom().item(routerBuilder.createRouter());
    }
    
    /**
     * With the provided Router, create an HTTP Server and mount the router
     * @param router A {@link Router} with the OpenAPI endpoints handled by the
     *               WebApiServices
     * @return A {@link Uni} which resolves to a {@link HttpServer} on success
     */
    Uni<HttpServer> createServer(Router router) {
        Router parentRouter = Router.router(vertx);
        parentRouter.route().handler(BodyHandler.create());
        parentRouter.route().handler(this::logAllRequests);
        parentRouter.mountSubRouter("/api/v1", router);
    
        LOG.info("HTTP Server Created");
        return vertx.createHttpServer().requestHandler(parentRouter).listen(8080);
    }
    
    /**
     * Log information about all requests
     * @param ctx The {@link RoutingContext} of each request
     */
    private void logAllRequests(RoutingContext ctx) {
        LOG.info("Path Params: {}", ctx.pathParams());
        LOG.info("Query Params: {}", ctx.queryParams());
        LOG.info("Metadata: {}", ctx.currentRoute().metadata());
        LOG.info("Body: {}", ctx.getBodyAsJson());
        ctx.next();
    }
    
    /**
     * Initialize and start this Verticle
     * @return A {@link Uni} which resolves successfully to a {@link Void} to indicate a successful start
     */
    @Override
    public Uni<Void> asyncStart() {
        LOG.info("Application initializing");
        
        // Configure the default Jackson object mapper
        var objectMapper = DatabindCodec.mapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        objectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        JavaTimeModule jsr310Module = new JavaTimeModule();
        objectMapper.registerModule(jsr310Module);
        
        // Create a deferred Uni wrapper around the blocking call to create the persistence context
        Uni<Mutiny.SessionFactory> createPersistenceContext = Uni.createFrom().deferred(MainVerticle::initHibernate);

        // Run the entire chain of async operations and resolve the Verticle startup
        return vertx.executeBlocking(createPersistenceContext)
                    .chain(this::initOpenAPI)
                    .chain(this::mapRoutes)
                    .chain(this::createServer)
                    .replaceWithVoid();
    }
}
