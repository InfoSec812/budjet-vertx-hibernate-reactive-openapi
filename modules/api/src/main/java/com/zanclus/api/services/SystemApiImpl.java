package com.zanclus.api.services;

import com.zanclus.models.Errors;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import org.hibernate.reactive.mutiny.Mutiny;

public class SystemApiImpl implements SystemApi, ServiceInterface {
    
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
        Errors healthStatus = new Errors();
        healthStatus.setCode(500);
        healthStatus.setMsg("Not ready");
        UniHelper.toFuture(sessionFactory.withSession(
                    session -> session.createNativeQuery("SELECT 1").getResultList())
               .map(o -> {
                   healthStatus.setCode(200);
                   healthStatus.setMsg("Ready");
                   return healthStatus;
               })
               .map(h -> ServiceResponse.completedWithJson(healthStatus.toJson())
           )
        ).onComplete(handler);
    }

    @Override
    public void getCurrentUser(ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {

    }
}
