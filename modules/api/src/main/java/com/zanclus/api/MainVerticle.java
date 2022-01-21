package com.zanclus.api;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Main Vert.x Verticle, entrypoint for this application
 */
public class MainVerticle extends AbstractVerticle {

	@Override
	public Uni<Void> asyncStart() {
		Uni<Mutiny.SessionFactory> startHibernate = Uni.createFrom().deferred(() -> {
			Mutiny.SessionFactory emf = Persistence
					.createEntityManagerFactory("dev")
					.unwrap(Mutiny.SessionFactory.class);
			
			return Uni.createFrom().item(emf);
		});
		
		return vertx.executeBlocking(startHibernate)
								.chain(emf -> emf.withSession(session -> session.createQuery("select i from Income i").getResultList()))
								.replaceWithVoid();
	}
}
