package attune;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AttuneApplication {

	public static void main(String[] args) {
		SpringApplication.run(AttuneApplication.class, args);
	}

}
