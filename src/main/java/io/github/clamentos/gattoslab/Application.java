package io.github.clamentos.gattoslab;

///
import java.io.IOException;
import java.io.PrintStream;

///.
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

///
@SpringBootApplication
@EnableScheduling

///
public class Application {

	///
	public static void main(String[] args) throws IOException {

		System.setOut(new PrintStream("./fake_stdout.log"));
		SpringApplication.run(Application.class, args);
	}

	///
}
