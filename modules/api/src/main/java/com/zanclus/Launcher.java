package com.zanclus;

public class Launcher extends io.vertx.core.Launcher {
	
	public static void main(String[] args) {
		System.setProperty("vertx.setClassPathResolvingEnabled", "true");
		System.setProperty("vertx.setFileCachingEnabled", "true");
		
		new io.vertx.core.Launcher().dispatch(args);
	}
}
