package com.zanclus.api.services;

import com.zanclus.models.Bill;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BillsApiImpl implements BillsApi, ServiceInterface {
    
    private static final Logger LOG = LoggerFactory.getLogger(BillsApiImpl.class);

    final Vertx vertx;
    
    Mutiny.SessionFactory sessionFactory;

    public void setSessionFactory(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    public BillsApiImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void addBill(JsonObject body, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        LOG.info("JSON Body: {}", body);
        var parsedNewBill = body.mapTo(Bill.class);
        UniHelper.toFuture(sessionFactory.withTransaction((session, tx) -> session.merge(parsedNewBill))
                .replaceWith(() -> parsedNewBill)
                .map(this::mapEntityToServiceResponse)
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
        LOG.info("Made it to the implementation");
        Functions.Function3<LocalDate, LocalDate, Handler<AsyncResult<ServiceResponse>>, Future<ServiceResponse>> fun =
            (LocalDate start, LocalDate end, Handler<AsyncResult<ServiceResponse>> hdlr) ->
                  UniHelper.toFuture(sessionFactory
                    .withSession(session -> session.createNamedQuery("getBillsForPeriod")
                                .setParameter(1, start).setParameter(2, end).getResultList())
                                .map(this::mapRawObjectToJsonObject)
                                .map(this::mapJsonObjectToBill)
                                .map(this::mapListToServiceResponse)
                      .onFailure()
                    .recoverWithItem(this::mapThrowableToServiceResponse)).onComplete(handler);
        LOG.info("Successfully defined the lambda");
        checkDates(startDate, endDate, fun, handler);
    }
    
    private List<Bill> mapJsonObjectToBill(List<JsonObject> jsonObjects) {
        return jsonObjects.stream()
                   .map(j -> j.mapTo(Bill.class))
                   .toList();
    }
    
    @Override
    public void updateBill(String id, JsonObject body, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        var parsedBill = body.mapTo(Bill.class);
        if (parsedBill.getId().toString().contentEquals(id)) {
            UniHelper.toFuture(sessionFactory.withTransaction(
                    (session, tx) -> session.merge(parsedBill)
                )
                .map(this::mapEntityToServiceResponse)
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse)).onComplete(handler);
        } else {
            handleBadRequest(handler);
        }
    }
    
    @Override
    public void getBill(String id, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        UniHelper.toFuture(sessionFactory.withSession(
                        session -> session.find(Bill.class, UUID.fromString(id))
                    )
                .map(this::mapEntityToServiceResponse)
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse))
            .onComplete(handler);
    }
}
