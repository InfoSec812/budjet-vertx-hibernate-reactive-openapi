package com.zanclus.api.services;

import com.zanclus.models.Bill;
import com.zanclus.models.Errors;
import com.zanclus.models.NewBill;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.persistence.NoResultException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BillsApiImpl implements BillsApi {
    
    Mutiny.SessionFactory sessionFactory;

    public BillsApiImpl(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private ServiceResponse mapListToServiceResponse(List<? extends Serializable> a) {
        List<JsonObject> results = a.stream().map(JsonObject::mapFrom).toList();
        return ServiceResponse.completedWithJson(new JsonArray(results));
    }

    private ServiceResponse mapEntityToServiceResponse(Serializable b) {
        return ServiceResponse.completedWithJson(JsonObject.mapFrom(b));
    }

    private ServiceResponse mapThrowableToServiceResponse(Throwable t) {
        Errors err = new Errors();
        err.setMsg(t.getLocalizedMessage());
        err.setCode(500);
        err.setTimestamp(LocalDateTime.now());
        return ServiceResponse.completedWithJson(JsonObject.mapFrom(err));
    }

    private ServiceResponse mapNoResultToNotFound(Throwable e) {
        var err = new Errors().code(404).msg("Not found").timestamp(LocalDateTime.now());
        return ServiceResponse.completedWithJson(JsonObject.mapFrom(err));
    }

    private Uni<? extends Serializable> mapNullToNotFound(Serializable e) {
        if (e == null) {
            return Uni.createFrom().failure(new NoResultException());
        }
        return Uni.createFrom().item(e);
    }

    private ServiceResponse mapToNoContentResponse(Void v) {
        return new ServiceResponse().setStatusCode(204).setStatusMessage("NO CONTENT");
    }

    @Override
    public void addBill(NewBill newBill, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        UniHelper.toFuture(sessionFactory.withSession(session -> session.persist(newBill)).replaceWith(newBill)
                .map(this::mapEntityToServiceResponse).map(sr -> sr.setStatusCode(200).setStatusMessage("OK"))
                .onFailure().recoverWithItem(this::mapThrowableToServiceResponse)).onComplete(handler);
    }

    @Override
    public void deleteBill(String id, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        UniHelper
                .toFuture(sessionFactory.withSession(
                        session -> session.find(Bill.class, UUID.fromString(id)).chain(this::mapNullToNotFound)
                                .chain(session::remove).chain(session::flush).map(this::mapToNoContentResponse)
                                .onFailure(NoResultException.class).recoverWithItem(this::mapNoResultToNotFound)))
                .onComplete(handler);
    }

    @Override
    public void getAllBills(LocalDate startDate, LocalDate endDate, ServiceRequest ctx,
            Handler<AsyncResult<ServiceResponse>> handler) {
        final var now = LocalDate.now();
        
        if (startDate == null) {
            startDate = now.minusDays(now.getDayOfMonth() - 1L);
        }
        
        if (endDate == null) {
            endDate = now.plusMonths(3);
        }
        
        if (endDate.isBefore(startDate)) {
            JsonObject errBody = new JsonObject();
            errBody.put("message", "End data cannot be before start date in query parameters");
            errBody.put("code", 400);
            ServiceResponse badRequest = new ServiceResponse();
            badRequest.setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
            badRequest.setStatusMessage(HttpResponseStatus.BAD_REQUEST.reasonPhrase());
            badRequest.setPayload(errBody.toBuffer());
            handler.handle(Future.succeededFuture(badRequest));
        } else {
            final LocalDate finalStartDate = startDate;
            final LocalDate finalEndDate = endDate;
            UniHelper.toFuture(sessionFactory
                    .withSession(session -> session.createNamedQuery("getBillsForPeriod", Bill.class)
                        .setParameter(1, finalStartDate).setParameter(2, finalEndDate).getResultList())
                    .map(this::mapListToServiceResponse).onFailure().recoverWithItem(this::mapThrowableToServiceResponse))
                .onComplete(handler);
        }
    }
}
