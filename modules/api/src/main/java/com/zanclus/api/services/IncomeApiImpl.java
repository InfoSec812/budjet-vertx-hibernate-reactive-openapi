package com.zanclus.api.services;

import com.zanclus.models.Income;
import com.zanclus.models.NewIncome;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;

public class IncomeApiImpl implements IncomeApi {
    @Override
    public void addIncomeSource(NewIncome newIncome, ServiceRequest ctx,
            Handler<AsyncResult<ServiceResponse>> handler) {

    }

    @Override
    public void deleteIncome(String id, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {

    }

    @Override
    public void getIncome(String id, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
        ServiceResponse response = new ServiceResponse().setStatusMessage("Not yet implemented").setStatusCode(501);
        var res = Future.succeededFuture(response);
        handler.handle(res);
    }

    @Override
    public void getIncomeSources(ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {

    }

    @Override
    public void updateIncome(String id, Income income, ServiceRequest ctx,
            Handler<AsyncResult<ServiceResponse>> handler) {

    }
}
