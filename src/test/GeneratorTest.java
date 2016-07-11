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
	public void basic(){
		System.out.println("----Testing Basic----");
		Program prog;
		try {
			prog = compile("basic");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "");
			System.out.println(out);
			assertTrue("If fault", out.contains("c: b"));
			assertTrue("Increment fault", out.contains("i: 5"));
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
	public void readPrint() {
		System.out.println("----Testing Read Print----");
		Program prog;
		try {
			prog = compile("readPrint");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "4\n5");
			System.out.println(out);
			assertTrue("'a' probably not eddited", out.contains("a : 4"));
			assertTrue("'b' probably not eddited", out.contains("b : 5"));
			assertTrue("Read 'a' fault", out.contains("c: 4"));
			assertTrue("Read 'b' fault", out.contains("d: 5"));
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
	public void arrithmeticPlus(){
		System.out.println("----Testing Plus Min----");
		Program prog;
		try{
			prog = compile("arithmeticPlus");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "");
			System.out.println(out);
			assertTrue("Plus fault", out.contains("c: 9"));
			assertTrue("Min min fault", out.contains("d: 9"));
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
	public void arrithmeticMult(){
		System.out.println("----Testing Mult Div Mod----");
		Program prog;
		try{
			prog = compile("arithmeticMult");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "");
			System.out.println(out);
			assertTrue("Mult fault", out.contains("d: 20"));
			assertTrue("Mod fault", out.contains("e: 1"));
			assertTrue("Div fault", out.contains("f: 4"));
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
	public void assDecl(){
		System.out.println("----Testing Assign Decleration----");
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
	public void compBool(){
		System.out.println("----Testing Comp Bool----");
		Program prog;
		try{
			prog = compile("compBool");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "");
			System.out.println(out);
			assertTrue("Plus fault", out.contains("c : -1"));
			assertTrue("Plus fault", out.contains("d : -1"));
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
	public void ifWhile(){
		System.out.println("----Testing If While----");
		Program prog;
		try{
			prog = compile("ifWhile");
			System.out.println(prog.prettyPrint());
			String out = sim(prog, "");
			System.out.println(out);
			assertTrue("Plus fault", out.contains("res0: 8"));
			assertTrue("Plus fault", out.contains("res1: 11"));
			assertTrue("Plus fault", out.contains("res2: 32"));
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
