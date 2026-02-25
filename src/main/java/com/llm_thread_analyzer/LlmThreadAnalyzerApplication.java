package com.llm_thread_analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class LlmThreadAnalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LlmThreadAnalyzerApplication.class, args);
	}

}