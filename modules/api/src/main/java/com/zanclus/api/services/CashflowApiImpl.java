package com.zanclus.api.services;

import io.smallrye.mutiny.tuples.Functions;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import org.hibernate.reactive.mutiny.Mutiny;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CashflowApiImpl extends AbstractService implements CashflowApi {
    
    final Vertx vertx;
    
    Mutiny.SessionFactory sessionFactory;
    
    public void setSessionFactory(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public CashflowApiImpl(Vertx vertx) {
        this.vertx = vertx;
    }
    
    private List<JsonObject> mapRawObjectToJsonObject(List<Object> objects) {
        return objects.stream()
            .map(o -> new JsonObject((String)o))
            .toList();
    }
    
    @Override
    public void getCashFlow(String startDate, String endDate, Float startingBalance, ServiceRequest ctx,
            Handler<AsyncResult<ServiceResponse>> handler) {
        var parsedStartDate = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        var parsedEndDate = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
    
    
        Functions.Function3<LocalDate, LocalDate, Handler<AsyncResult<ServiceResponse>>, Future<ServiceResponse>> fun =
            (LocalDate start, LocalDate end, Handler<AsyncResult<ServiceResponse>> hdlr) ->
                UniHelper.toFuture(sessionFactory
                    .withSession(session ->
                         session
                                .createNamedQuery("getCashFlowForDateRangeAndStartingBalance")
                                .setParameter(1, start)
                                .setParameter(2, end)
                                .setParameter(3, startingBalance)
                                .getResultList()
                    )
                    .map(this::mapRawObjectToJsonObject)
                    .map(this::mapJsonListToServiceResponse).onFailure()
                    .recoverWithItem(this::mapThrowableToServiceResponse)).onComplete(handler);
        checkDates(parsedStartDate, parsedEndDate, fun, handler);
    }
}
