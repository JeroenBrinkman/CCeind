package test;

import static org.junit.Assert.*;
import iloc.Simulator;
import iloc.model.Program;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.junit.Test;

import checker.ParseException;
import compiler.TempNameCompiler;

public class programsTest {
	private final static String BASE_DIR = "src/files";
	private final static String EXT = ".tmp";
	private final TempNameCompiler compiler = TempNameCompiler.instance();

	@Test
	public void gcd() {
		System.out.println("----Testing GCD----");
		Program prog;
		try {
			prog = compile("gcd");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "100\n170");
			System.out.println(out);
			
		} catch (IOException e) {
			fail("Read/Write fault");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for (String msg : e.getMessages()) {
				System.err.println(msg);
			}
			fail("An error occured, see command line for more information.");
		}
	}
	
	@Test
	public void fib() {
		System.out.println("----Testing Fib----");
		Program prog;
		try {
			prog = compile("fib");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "8");
			System.out.println(out);
			
		} catch (IOException e) {
			fail("Read/Write fault");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for (String msg : e.getMessages()) {
				System.err.println(msg);
			}
			fail("An error occured, see command line for more information.");
		}
	}

	private Program compile(String filename) throws IOException, ParseException {
		return this.compiler.compile(new File(BASE_DIR, filename + EXT));
	}

	private String sim(Program prog, String input) {
		Simulator sim = new Simulator(prog);
		sim.setIn(new ByteArrayInputStream(input.getBytes()));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		sim.setOut(out);
		sim.run();
		return out.toString();
	}

}
