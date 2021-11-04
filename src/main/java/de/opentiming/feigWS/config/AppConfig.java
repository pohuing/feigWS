package de.opentiming.feigWS.config;

import de.opentiming.feigWS.reader.BrmReadThread;
import de.opentiming.feigWS.reader.FedmConnect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class AppConfig {
	
    @Bean
    public Map<String, FedmConnect> connections() {
    	Map<String, FedmConnect> connections = new HashMap<>();
		return connections;
    }
    
    @Bean
    public Map<String, BrmReadThread> brmthreads() {
    	Map<String, BrmReadThread> brmthreads = new HashMap<>();
		return brmthreads;
    }

}