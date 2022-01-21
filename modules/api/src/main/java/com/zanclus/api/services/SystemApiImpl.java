package com.zanclus.api.services;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;

public class SystemApiImpl implements SystemApi {
	@Override
	public void checkHealth(ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
	
	}
	
	@Override
	public void getCurrentUser(ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
	
	}
}
