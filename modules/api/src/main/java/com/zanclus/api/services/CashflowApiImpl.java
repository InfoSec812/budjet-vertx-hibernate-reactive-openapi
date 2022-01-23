package com.zanclus.api.services;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CashflowApiImpl implements CashflowApi {
    @Override
    public void getCashFlow(String startDate, String endDate, Float startingBalance, ServiceRequest ctx,
            Handler<AsyncResult<ServiceResponse>> handler) {
        var parsedStartDate = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        var parsedEndDate = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);

    }
}
