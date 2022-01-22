package com.zanclus.api.services;

import com.zanclus.models.Bill;
import com.zanclus.models.Income;
import com.zanclus.models.NewIncome;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Functions;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.persistence.NoResultException;
import java.time.LocalDate;
import java.util.UUID;

public class IncomeApiImpl extends AbstractService implements IncomeApi {

    Mutiny.SessionFactory sessionFactory;

    public IncomeApiImpl(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void addIncomeSource(NewIncome newIncome, ServiceRequest ctx,
            Handler<AsyncResult<ServiceResponse>> handler) {
        UniHelper.toFuture(sessionFactory.withSession(session -> session.persist(newIncome)).replaceWith(newIncome)
                .map(this::mapEntityToServiceResponse).map(sr -> sr.setStatusCode(200).setStatusMessage("OK"))
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse)).onComplete(handler);
    }

    @Override
    public void deleteIncome(String id, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        UniHelper
                .toFuture(sessionFactory.withSession(
                        session -> session.find(Income.class, UUID.fromString(id)).chain(this::mapNullToNotFound)
                                .chain(session::remove).chain(session::flush).map(this::mapToNoContentResponse)
                                .onFailure(NoResultException.class).recoverWithItem(this::mapNoResultToNotFound)))
                .onComplete(handler);
    }

    @Override
    public void getIncome(String id, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        ServiceResponse response = new ServiceResponse().setStatusMessage("Not yet implemented").setStatusCode(501);
        var res = Future.succeededFuture(response);
        handler.handle(res);
    }

    @Override
    public void getIncomeSources(LocalDate startDate, LocalDate endDate, ServiceRequest ctx,
            Handler<AsyncResult<ServiceResponse>> handler) {
        
        Functions.Function3<LocalDate, LocalDate, Handler<AsyncResult<ServiceResponse>>, Future<ServiceResponse>> fun =
            (LocalDate start, LocalDate end, Handler<AsyncResult<ServiceResponse>> hdlr) -> UniHelper.toFuture(sessionFactory
                .withSession(session -> session.createNamedQuery("getIncomeForPeriod", Bill.class)
                    .setParameter(1, start).setParameter(2, end).getResultList())
                .map(this::mapListToServiceResponse).onFailure()
                .recoverWithItem(this::mapThrowableToServiceResponse)).onComplete(handler);
        checkDates(startDate, endDate, fun, handler);
    }
    
    @Override
    public void updateIncome(String id, Income income, ServiceRequest ctx,
            Handler<AsyncResult<ServiceResponse>> handler) {
        if (income.getId().toString().contentEquals(id)) {
            UniHelper.toFuture(sessionFactory.withSession(
                    session -> session.merge(income)
                )
                .map(this::mapEntityToServiceResponse)
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse)).onComplete(handler);
        } else {
            handleBadRequest(handler);
        }
    }
}
