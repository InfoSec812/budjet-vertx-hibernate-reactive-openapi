package com.zanclus.api.services;

import com.zanclus.models.Errors;
import com.zanclus.models.User;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
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
    public Future<ServiceResponse> checkHealth(ServiceRequest ctx) {
        Errors healthStatus = new Errors();
        healthStatus.setCode(500);
        healthStatus.setMsg("Not ready");
        return UniHelper.toFuture(sessionFactory.withSession(
                    session -> session.createNativeQuery("SELECT 1").getSingleResult())
               .map(o -> {
                   healthStatus.setCode(200);
                   healthStatus.setMsg("Ready");
                   return healthStatus;
               })
               .map(h -> ServiceResponse.completedWithJson(healthStatus.toJson())
           )
        );
    }

    @Override
    public Future<ServiceResponse> getCurrentUser(ServiceRequest ctx) {
      var staticUser = new User()
                                    .email("me@example.com")
                                    .name("John Doe");
      var currentUserInfo = ServiceResponse.completedWithJson(staticUser.toJson());
      return Future.succeededFuture(currentUserInfo);
    }
}
