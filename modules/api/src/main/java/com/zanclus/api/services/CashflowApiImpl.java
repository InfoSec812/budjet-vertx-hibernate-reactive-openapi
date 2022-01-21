package com.zanclus.api.services;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CashflowApiImpl implements CashflowApi {
	@Override
	public void getCashFlow(LocalDate startDate, LocalDate endDate, BigDecimal startingBalance, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
	
	}
}
