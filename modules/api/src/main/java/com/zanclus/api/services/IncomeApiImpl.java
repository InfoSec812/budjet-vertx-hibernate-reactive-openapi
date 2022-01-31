package com.zanclus.api.services;

import com.zanclus.models.Bill;
import com.zanclus.models.Income;
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

import javax.persistence.NoResultException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.BiFunction;

public class IncomeApiImpl implements IncomeApi, ServiceInterface {
    
    final Vertx vertx;
    
    Mutiny.SessionFactory sessionFactory;
    
    public void setSessionFactory(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public IncomeApiImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public Future<ServiceResponse> addIncomeSource(JsonObject body, ServiceRequest ctx) {
        var parsedNewIncome = body.mapTo(Income.class);
        return UniHelper.toFuture(sessionFactory.withTransaction((session, tx) ->
                                          session.merge(parsedNewIncome))
                .replaceWith(() -> parsedNewIncome)
                .map(this::mapEntityToServiceResponse).map(sr -> sr.setStatusCode(200).setStatusMessage("OK"))
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse));
    }

    @Override
    public Future<ServiceResponse> deleteIncome(String id, ServiceRequest ctx) {
        return UniHelper
                .toFuture(sessionFactory.withSession(
                        session -> session.find(Income.class, UUID.fromString(id))
                            .chain(this::mapNullToNotFound)
                            .chain(session::remove)
                            .chain(session::flush)
                            .map(this::mapToNoContentResponse)
                            .onFailure(NoResultException.class).recoverWithItem(this::mapNoResultToNotFound)));
    }

    @Override
    public Future<ServiceResponse> getIncome(String id, ServiceRequest ctx) {
        return UniHelper.toFuture(sessionFactory.withSession(session ->
                                      session.find(Income.class, UUID.fromString(id))
                                 .map(this::mapEntityToServiceResponse)
                                 .onFailure().recoverWithItem(this::mapThrowableToServiceResponse)));
    }

    @Override
    public Future<ServiceResponse> getIncomeSources(String startDate, String endDate, ServiceRequest ctx) {
        
        BiFunction<LocalDate, LocalDate, Future<ServiceResponse>> fun =
            (LocalDate start, LocalDate end) -> UniHelper.toFuture(sessionFactory
                .withSession(session -> session.createNamedQuery("getIncomeForPeriod", Bill.class)
                    .setParameter(1, start).setParameter(2, end).getResultList())
                .map(this::mapListToServiceResponse).onFailure()
                .recoverWithItem(this::mapThrowableToServiceResponse));
        return checkDates(startDate, endDate, fun);
    }
    
    @Override
    public Future<ServiceResponse> updateIncome(String id, JsonObject body, ServiceRequest ctx) {
        var parsedIncome = body.mapTo(Income.class);
        if (parsedIncome.getId().toString().contentEquals(id)) {
            return UniHelper.toFuture(sessionFactory.withTransaction(
                    (session, tx) -> session.merge(parsedIncome)
                )
                .map(this::mapEntityToServiceResponse)
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse));
        } else {
            return handleBadRequest();
        }
    }
}
