package com.zanclus;

public class BudjetLauncher extends io.vertx.core.Launcher {
	
	public static void main(String[] args) {
		System.setProperty("vertx.setClassPathResolvingEnabled", "true");
		System.setProperty("vertx.setFileCachingEnabled", "true");
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		
		new io.vertx.core.Launcher().dispatch(args);
	}
}
