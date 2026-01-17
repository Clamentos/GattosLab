package io.github.clamentos.gattoslab;

///
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

///.
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

///
@SpringBootApplication

///
public class Application {

	///
	public static void main(final String[] args) throws IOException {

		final PrintStream consoleOut = new PrintStream("./console_out.log");

		System.setOut(consoleOut);
		System.setErr(consoleOut);

		try(final FileWriter pidFile = new FileWriter("./pid.txt")) {

			pidFile.write(Long.toString(ProcessHandle.current().pid()));
		}

		SpringApplication.run(Application.class, args);
	}

	///
}
