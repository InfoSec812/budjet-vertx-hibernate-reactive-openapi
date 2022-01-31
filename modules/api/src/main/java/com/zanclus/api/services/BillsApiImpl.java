package com.zanclus.api.services;

import com.zanclus.models.Bill;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Future;
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
import java.util.function.BiFunction;

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
    public Future<ServiceResponse> addBill(JsonObject body, ServiceRequest ctx) {
        LOG.info("JSON Body: {}", body);
        var parsedNewBill = body.mapTo(Bill.class);
        return UniHelper.toFuture(sessionFactory.withTransaction((session, tx) -> session.merge(parsedNewBill))
                .replaceWith(() -> parsedNewBill)
                .map(this::mapEntityToServiceResponse)
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse));
    }

    @Override
    public Future<ServiceResponse> deleteBill(String id, ServiceRequest ctx) {
        return UniHelper
                .toFuture(sessionFactory.withSession(
                    session -> session.find(Bill.class, UUID.fromString(id))
                        .chain(this::mapNullToNotFound)
                        .chain(session::remove).chain(session::flush)
                        .map(this::mapToNoContentResponse)
                        .onFailure(NoResultException.class).recoverWithItem(this::mapNoResultToNotFound)));
    }

    @Override
    public Future<ServiceResponse> getAllBills(String startDate, String endDate, ServiceRequest ctx) {
        LOG.info("Made it to the implementation");
        BiFunction<LocalDate, LocalDate, Future<ServiceResponse>> fun =
            (LocalDate start, LocalDate end) ->
                  UniHelper.toFuture(sessionFactory
                    .withSession(session -> session.createNamedQuery("getBillsForPeriod")
                                .setParameter(1, start).setParameter(2, end).getResultList())
                                .map(this::mapRawObjectToJsonObject)
                                .map(this::mapJsonObjectToBill)
                                .map(this::mapListToServiceResponse)
                      .onFailure()
                    .recoverWithItem(this::mapThrowableToServiceResponse));
        LOG.info("Successfully defined the lambda");
        return checkDates(startDate, endDate, fun);
    }
    
    private List<Bill> mapJsonObjectToBill(List<JsonObject> jsonObjects) {
        return jsonObjects.stream()
                   .map(j -> j.mapTo(Bill.class))
                   .toList();
    }
    
    @Override
    public Future<ServiceResponse> updateBill(String id, JsonObject body, ServiceRequest ctx) {
        var parsedBill = body.mapTo(Bill.class);
        if (parsedBill.getId().toString().contentEquals(id)) {
            return UniHelper.toFuture(sessionFactory.withTransaction(
                    (session, tx) -> session.merge(parsedBill)
                )
                .map(this::mapEntityToServiceResponse)
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse));
        } else {
            return handleBadRequest();
        }
    }
    
    @Override
    public Future<ServiceResponse> getBill(String id, ServiceRequest ctx) {
        return UniHelper.toFuture(sessionFactory.withSession(
                        session -> session.find(Bill.class, UUID.fromString(id))
                    )
                .map(this::mapEntityToServiceResponse)
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse));
    }
}
