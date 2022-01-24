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
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public interface ServiceInterface {
    
    default ServiceResponse mapListToServiceResponse(List<? extends Serializable> a) {
        List<JsonObject> results = a.stream().map(JsonObject::mapFrom).toList();
        return ServiceResponse.completedWithJson(new JsonArray(results));
    }
    
    default ServiceResponse mapEntityToServiceResponse(Serializable b) {
        return ServiceResponse.completedWithJson(JsonObject.mapFrom(b));
    }
    
    default ServiceResponse mapThrowableToServiceResponse(Throwable t) {
        Errors err = new Errors();
        err.setMsg(t.getLocalizedMessage());
        err.setCode(500);
        err.setTimestamp(OffsetDateTime.now());
        return ServiceResponse.completedWithJson(JsonObject.mapFrom(err));
    }
    
    default ServiceResponse mapNoResultToNotFound(Throwable e) {
        var err = new Errors().code(404).msg("Not found").timestamp(OffsetDateTime.now());
        return ServiceResponse.completedWithJson(JsonObject.mapFrom(err));
    }
    
    default List<JsonObject> mapRawObjectToJsonObject(List<Object> objects) {
        return objects.stream()
            .map(o -> new JsonObject((String)o))
            .toList();
    }
    
    default Uni<Serializable> mapNullToNotFound(Serializable e) {
        if (e == null) {
            return Uni.createFrom().failure(new NoResultException());
        }
        return Uni.createFrom().item(e);
    }
    
    default ServiceResponse mapToNoContentResponse(Void v) {
        return new ServiceResponse().setStatusCode(204).setStatusMessage("NO CONTENT");
    }
    
    default void checkDates(String startDate, String endDate, Functions.Function3<LocalDate, LocalDate, Handler<AsyncResult<ServiceResponse>>, Future<ServiceResponse>> fun, Handler<AsyncResult<ServiceResponse>> handler) {
        final var now = LocalDate.now();
        LocalDate parsedStartDate;
        LocalDate parsedEndDate;
        
        if (startDate == null) {
            parsedStartDate = now.minusDays(now.getDayOfMonth() - 1L);
        } else {
            try {
                parsedStartDate = LocalDate.parse(startDate);
            } catch(DateTimeParseException e) {
                parsedStartDate = now.minusDays(now.getDayOfMonth() - 1L);
            }
        }
        
        if (endDate == null) {
            parsedEndDate = now.plusMonths(3);
        } else {
            try {
                parsedEndDate = LocalDate.parse(endDate);
            } catch(DateTimeParseException e) {
                parsedEndDate = now.plusMonths(3);
            }
        }
        
        if (parsedEndDate.isBefore(parsedStartDate)) {
            handleBadRequest(handler);
        } else {
            fun.apply(parsedStartDate, parsedEndDate, handler);
        }
    }
    
    default void handleBadRequest(Handler<AsyncResult<ServiceResponse>> handler) {
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
