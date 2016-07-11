package test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.junit.Test;

import checker.ParseException;
import compiler.TempNameCompiler;
import iloc.Simulator;
import iloc.model.Program;

public class StringTest {
	private final static String BASE_DIR = "src/files";
	private final static String EXT = ".tmp";
	private final TempNameCompiler compiler = TempNameCompiler.instance();

	@Test
	public void test0() throws IOException, ParseException {
		Program prog = compile("sttest0");
		System.out.println(prog.prettyPrint());
		String out = sim(prog, "");
		System.out.println(out);

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
