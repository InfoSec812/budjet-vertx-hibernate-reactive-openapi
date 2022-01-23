package com.zanclus.api.services;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import org.hibernate.reactive.mutiny.Mutiny;

public class SystemApiImpl implements SystemApi {
    
    final Vertx vertx;
    
    Mutiny.SessionFactory sessionFactory;
    
    public void setSessionFactory(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public SystemApiImpl(Vertx vertx) {
        this.vertx = vertx;
    }
    
    @Override
    public void checkHealth(ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {

    }

    @Override
    public void getCurrentUser(ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {

    }
}
