package com.mingle.a2srv_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class A2srvServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(A2srvServiceApplication.class, args);
	}

}
