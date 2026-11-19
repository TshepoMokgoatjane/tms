package za.co.tms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// CORS is configured in one place only: za.co.tms.config.CorsConfig (applied by the security chain).
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class TmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(TmsApplication.class, args);
	}
}
