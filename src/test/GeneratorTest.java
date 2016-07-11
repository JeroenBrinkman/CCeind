package test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.junit.Test;

import checker.ParseException;
import iloc.Simulator;
import iloc.model.Program;
import compiler.TempNameCompiler;

public class GeneratorTest {
	private final static String BASE_DIR = "src/files/GeneratorTestFiles";
	private final static String EXT = ".tmp";
	private final TempNameCompiler compiler = TempNameCompiler.instance();

	@Test
	public void readPrint(){
		Program prog;
		try {
			prog = compile("readPrint");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "4\n5");
			System.out.println(out);
		} catch (IOException e) {
			fail("Read/Write fault");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for(String msg : e.getMessages()){
				System.err.println(msg);
			}
			fail("An error occured, see command line for more information.");
		}
		

	}
	
	@Test
	public void arrithmeticPlus() throws IOException, ParseException {
		Program prog;
		try{
			prog = compile("arithmeticPlus");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "");
			System.out.println(out);
		} catch (IOException e) {
			fail("Read/Write fault");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for(String msg : e.getMessages()){
				System.err.println(msg);
			}
			fail("An error occured, see command line for more information.");
		}
	}

	@Test
	public void arrithmeticMult() throws IOException, ParseException {
		Program prog;
		try{
			prog = compile("arithmeticMult");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "");
			System.out.println(out);
		} catch (IOException e) {
			fail("Read/Write fault");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for(String msg : e.getMessages()){
				System.err.println(msg);
			}
			fail("An error occured, see command line for more information.");
		}
	}

	@Test
	public void assDecl() throws IOException, ParseException {
		Program prog;
		try{
			prog = compile("assDecl");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "");
			System.out.println(out);
		} catch (IOException e) {
			fail("Read/Write fault");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for(String msg : e.getMessages()){
				System.err.println(msg);
			}
			fail("An error occured, see command line for more information.");
		}
	}

	@Test
	public void genTest0() throws IOException, ParseException {
		Program prog;
		try{
			prog = compile("compBool");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "");
			System.out.println(out);
		} catch (IOException e) {
			fail("Read/Write fault");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for(String msg : e.getMessages()){
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
