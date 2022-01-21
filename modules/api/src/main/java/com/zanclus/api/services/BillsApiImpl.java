package com.zanclus.api.services;

import com.zanclus.models.Bill;
import com.zanclus.models.Errors;
import com.zanclus.models.NewBill;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.service.ServiceRequest;
import io.vertx.ext.web.api.service.ServiceResponse;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.persistence.NoResultException;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.UUID;

import static com.zanclus.models.Period.*;

public class BillsApiImpl implements BillsApi {
	
	Mutiny.SessionFactory sessionFactory;
	
	public BillsApiImpl(Mutiny.SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	private ServiceResponse mapEntityToServiceResponse(JsonArray array) {
		return ServiceResponse.completedWithJson(array);
	}
	
	private ServiceResponse mapEntityToServiceResponse(Serializable b) {
		return ServiceResponse.completedWithJson(JsonObject.mapFrom(b));
	}
	
	private ServiceResponse mapThrowableToServiceResponse(Throwable t) {
		Errors err = new Errors();
		err.setMsg(t.getLocalizedMessage());
		err.setCode(500);
		err.setTimestamp(LocalDateTime.now());
		ServiceResponse res = ServiceResponse.completedWithJson(JsonObject.mapFrom(err));
		return res;
	}
	
	private ServiceResponse mapNoResultToNotFound(Throwable e) {
		var err = new Errors().code(404).msg("Not found").timestamp(LocalDateTime.now());
		var res = ServiceResponse.completedWithJson(JsonObject.mapFrom(err));
		return res;
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
		UniHelper.toFuture(
					sessionFactory.withSession(session -> session.persist(newBill))
												.replaceWith(newBill)
								        .map(this::mapEntityToServiceResponse)
							          .map((sr) -> sr.setStatusCode(200).setStatusMessage("OK"))
							          .onFailure().recoverWithItem(this::mapThrowableToServiceResponse)
				)
				.onComplete(handler::handle);
	}
	
	@Override
	public void deleteBill(String id, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
		UniHelper.toFuture(sessionFactory.withSession(session ->
					session.find(Bill.class, UUID.fromString(id))
							.chain(this::mapNullToNotFound)
							.chain(bill -> session.remove(bill))
							.chain(() -> session.flush())
							.map(this::mapToNoContentResponse)
							.onFailure(NoResultException.class).recoverWithItem(this::mapNoResultToNotFound)
					)
				)
				.onComplete(handler::handle);
	}
	
	@Override
	public void getAllBills(LocalDate startDate, LocalDate endDate, ServiceRequest ctx, Handler<AsyncResult<ServiceResponse>> handler) {
		final var query = "SELECT b.* FROM bills b";
		UniHelper.toFuture(sessionFactory.withSession(session ->
           session.createQuery(query, Bill.class).getResultList())
               .map(this::sortByNextDue)
               .map(JsonArray::new)
               .map(this::mapEntityToServiceResponse)
							 .onFailure().recoverWithItem(this::mapThrowableToServiceResponse)
						)
				.onComplete(handler::handle);
	}
	
	private List<Bill> sortByNextDue(List<Bill> bills) {
		bills.sort(this::dueDateComparator);
		return bills;
	}
	
	private int dueDateComparator(Bill a, Bill b) {
		
		
		return 0;
	}
	
	private long daysUntilNextDue(Bill bill) {
		switch(bill.getPeriodicity()) {
			case ONCE:
				return LocalDate.now().until(bill.getStartDate(), ChronoUnit.DAYS);
			case WEEKLY:
				var dow = bill.getStartDate().getDayOfWeek().getValue();
				var todayWeek = LocalDate.now().getDayOfWeek().getValue();
				var nextDueWeekly = dow - todayWeek;
				return nextDueWeekly >= 0 ? nextDueWeekly : nextDueWeekly + 7;
			case MONTHLY:
				var dom = bill.getStartDate().getDayOfMonth();
				var todayMonth = LocalDate.now().getDayOfMonth();
				var nextDueMonthly = dom - todayMonth;
				return nextDueMonthly >= 0 ? nextDueMonthly : (nextDueMonthly + LocalDate.now().getMonth().maxLength());
			case QUARTERLY:
				var startMon = bill.getStartDate().getMonth();
				var currMonth = LocalDate.now().getMonth();
				if (Math.abs(startMon.getValue() - currMonth.getValue()) % 3 == 0) {
					return bill.getDueDate();
				}
				break;
			case SEMIANUALLY:
				break;
			case ANNUALLY:
				break;
			default:
				break;
		}
		return 0;
	}
}
