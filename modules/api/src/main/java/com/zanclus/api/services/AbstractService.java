package com.zanclus.api.services;

import com.zanclus.models.Errors;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Functions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceResponse;

import javax.persistence.NoResultException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public abstract class AbstractService {

    ServiceResponse mapListToServiceResponse(List<? extends Serializable> a) {
        List<JsonObject> results = a.stream().map(JsonObject::mapFrom).toList();
        return ServiceResponse.completedWithJson(new JsonArray(results));
    }

    ServiceResponse mapEntityToServiceResponse(Serializable b) {
        return ServiceResponse.completedWithJson(JsonObject.mapFrom(b));
    }

    ServiceResponse mapThrowableToServiceResponse(Throwable t) {
        Errors err = new Errors();
        err.setMsg(t.getLocalizedMessage());
        err.setCode(500);
        err.setTimestamp(LocalDateTime.now());
        return ServiceResponse.completedWithJson(JsonObject.mapFrom(err));
    }

    ServiceResponse mapNoResultToNotFound(Throwable e) {
        var err = new Errors().code(404).msg("Not found").timestamp(LocalDateTime.now());
        return ServiceResponse.completedWithJson(JsonObject.mapFrom(err));
    }

    Uni<? extends Serializable> mapNullToNotFound(Serializable e) {
        if (e == null) {
            return Uni.createFrom().failure(new NoResultException());
        }
        return Uni.createFrom().item(e);
    }

    ServiceResponse mapToNoContentResponse(Void v) {
        return new ServiceResponse().setStatusCode(204).setStatusMessage("NO CONTENT");
    }
    
    void checkDates(LocalDate startDate, LocalDate endDate, Functions.Function3<LocalDate, LocalDate, Handler<AsyncResult<ServiceResponse>>, Future<ServiceResponse>> fun, Handler<AsyncResult<ServiceResponse>> handler) {
        final var now = LocalDate.now();
        
        if (startDate == null) {
            startDate = now.minusDays(now.getDayOfMonth() - 1L);
        }
        
        if (endDate == null) {
            endDate = now.plusMonths(3);
        }
        
        if (endDate.isBefore(startDate)) {
            handleBadRequest(handler);
        } else {
            fun.apply(startDate, endDate, handler);
        }
    }
    
    void handleBadRequest(Handler<AsyncResult<ServiceResponse>> handler) {
        ServiceResponse badRequest = new ServiceResponse();
        badRequest.setStatusMessage(HttpResponseStatus.BAD_REQUEST.reasonPhrase());
        badRequest.setStatusCode(HttpResponseStatus.BAD_REQUEST.code());
        JsonObject errBody = new JsonObject();
        errBody.put("message", "ID from path param MUST match ID in submitted object.");
        errBody.put("code", 400);
        badRequest.setPayload(errBody.toBuffer());
        handler.handle(Future.succeededFuture(badRequest));
    }
}
