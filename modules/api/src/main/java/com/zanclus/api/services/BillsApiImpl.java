package com.zanclus.api.services;

import com.zanclus.models.Bill;
import com.zanclus.models.NewBill;
import io.smallrye.mutiny.tuples.Functions;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.persistence.NoResultException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class BillsApiImpl extends AbstractService implements BillsApi {

    Mutiny.SessionFactory sessionFactory;

    public BillsApiImpl(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void addBill(JsonObject newBill, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        var parsedNewBill = newBill.mapTo(NewBill.class);
        UniHelper.toFuture(sessionFactory.withSession(session -> session.persist(parsedNewBill))
                .replaceWith(() -> parsedNewBill)
                .map(this::mapEntityToServiceResponse)
                .map(sr -> sr.setStatusCode(200).setStatusMessage("OK"))
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse))
            .onComplete(handler);
    }

    @Override
    public void deleteBill(String id, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        UniHelper
                .toFuture(sessionFactory.withSession(
                    session -> session.find(Bill.class, UUID.fromString(id))
                        .chain(this::mapNullToNotFound)
                        .chain(session::remove).chain(session::flush)
                        .map(this::mapToNoContentResponse)
                        .onFailure(NoResultException.class).recoverWithItem(this::mapNoResultToNotFound)))
                .onComplete(handler);
    }

    @Override
    public void getAllBills(String startDate, String endDate, ServiceRequest ctx,
            Handler<AsyncResult<ServiceResponse>> handler) {
        var parsedStartDate = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        var parsedEndDate = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
        Functions.Function3<LocalDate, LocalDate, Handler<AsyncResult<ServiceResponse>>, Future<ServiceResponse>> fun =
            (LocalDate start, LocalDate end, Handler<AsyncResult<ServiceResponse>> hdlr) ->
                  UniHelper.toFuture(sessionFactory
                    .withSession(session -> session.createNamedQuery("getBillsForPeriod", Bill.class)
                            .setParameter(1, start).setParameter(2, end).getResultList())
                    .map(this::mapListToServiceResponse).onFailure()
                    .recoverWithItem(this::mapThrowableToServiceResponse)).onComplete(handler);
        checkDates(parsedStartDate, parsedEndDate, fun, handler);
    }
    
    @Override
    public void updateBill(String id, JsonObject bill, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        var parsedBill = bill.mapTo(Bill.class);
        if (parsedBill.getId().toString().contentEquals(id)) {
            UniHelper.toFuture(sessionFactory.withSession(
                    session -> session.merge(parsedBill)
                )
                .map(this::mapEntityToServiceResponse)
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse)).onComplete(handler);
        } else {
            handleBadRequest(handler);
        }
    }
}
