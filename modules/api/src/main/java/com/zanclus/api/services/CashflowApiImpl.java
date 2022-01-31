package com.zanclus.api.services;

import com.zanclus.models.DailyBalance;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import org.hibernate.reactive.mutiny.Mutiny;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;

public class CashflowApiImpl implements CashflowApi, ServiceInterface {
    
    final Vertx vertx;
    
    Mutiny.SessionFactory sessionFactory;
    
    public void setSessionFactory(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public CashflowApiImpl(Vertx vertx) {
        this.vertx = vertx;
    }
    
    @Override
    public Future<ServiceResponse> getCashFlow(String startDate, String endDate, Float startingBalance, ServiceRequest ctx) {
    
        BiFunction<LocalDate, LocalDate, Future<ServiceResponse>> fun =
            (LocalDate start, LocalDate end) ->
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
                        .map(this::mapJsonObjectsToDailyBalance)
                        .map(this::mapListToServiceResponse)
                        .onFailure().recoverWithItem(this::mapThrowableToServiceResponse));
        return checkDates(startDate, endDate, fun);
    }
    
    private List<DailyBalance> mapJsonObjectsToDailyBalance(List<JsonObject> jsonObjects) {
        return jsonObjects
                    .stream()
                    .map(j -> j.mapTo(DailyBalance.class))
                    .toList();
    }
}
