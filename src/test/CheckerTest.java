package test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import checker.ParseException;
import checker.Result;
import compiler.TempNameCompiler;

public class CheckerTest {
	private final static String BASE_DIR = "src/files/TypeCheckerTestFiles";
	private final static String EXT = ".tmp";
	private final TempNameCompiler compiler = TempNameCompiler.instance();

	/**
	 * Tests that should get through the checker without errors.
	 */

	@SuppressWarnings("unused")
	@Test
	public void testBasic() {
		System.out.println("----Testing Basics----");
		ParseTree tree;
		Result result;
		try {
			tree = parse("basic");
			result = check(tree);
			System.out.println(tree.toStringTree());
		} catch (IOException e) {
			fail("Read/Write fault.");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for (String msg : e.getMessages()) {
				System.err.println(msg);
			}
			fail("An error occured, see command line for more information.");
		}

	}

	@SuppressWarnings("unused")
	@Test
	public void testOp() {
		System.out.println("----Testing Operations----");
		ParseTree tree;
		Result result;
		try {
			tree = parse("ops");
			result = check(tree);
			System.out.println(tree.toStringTree());
		} catch (IOException e) {
			fail("Read/Write fault.");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for (String msg : e.getMessages()) {
				System.err.println(msg);
			}
			fail("An error occured, see command line for more information.");
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testExpr() {
		System.out.println("----Testing Expressions----");
		ParseTree tree;
		Result result;
		try {
			tree = parse("exprs");
			result = check(tree);
			System.out.println(tree.toStringTree());
		} catch (IOException e) {
			fail("Read/Write fault.");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for (String msg : e.getMessages()) {
				System.err.println(msg);
			}
			fail("An error occured, see command line for more information.");
		}
	}

	/**
	 * Tests that should not get through the checker and thus produce errors.
	 * The errors that are printed are explained in
	 */
	@SuppressWarnings("unused")
	@Test
	public void testErr0() {
		System.out.println("----Testing Error 0----");
		ParseTree tree;
		Result result;
		try {
			tree = parse("err0");
			result = check(tree);
			System.out.println(tree.toStringTree());
			fail("No errors occured.");
		} catch (IOException e) {
			fail("Read/Write fault.");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for (String msg : e.getMessages()) {
				System.err.println(msg);
			}

		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testErr1() {
		System.out.println("----Testing Error 1----");
		ParseTree tree;
		Result result;
		try {
			tree = parse("err1");
			result = check(tree);
			System.out.println(tree.toStringTree());
			fail("No errors occured.");
		} catch (IOException e) {
			fail("Read/Write fault.");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for (String msg : e.getMessages()) {
				System.err.println(msg);
			}
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testErr2() {
		System.out.println("----Testing Error 2----");
		ParseTree tree;
		Result result;
		try {
			tree = parse("err2");
			result = check(tree);
			System.out.println(tree.toStringTree());
			fail("No errors occured.");
		} catch (IOException e) {
			fail("File error.");
		} catch (ParseException e) {
			System.err.println("Errors: ");
			for (String msg : e.getMessages()) {
				System.err.println(msg);
			}

		}
	}

	private ParseTree parse(String filename) throws IOException, ParseException {
		return this.compiler.parse(new File(BASE_DIR, filename + EXT));
	}

	private Result check(ParseTree tree) throws ParseException {

		return this.compiler.check(tree);
	}
}
