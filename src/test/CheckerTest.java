package test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;

import checker.ParseException;
import checker.Result;
import checker.Type;
import compiler.TempNameCompiler;

public class CheckerTest {
	private final static String BASE_DIR = "src/files";
	private final static String EXT = ".tmp";
	private final TempNameCompiler compiler = TempNameCompiler.instance();

	@Test
	public void test0() throws ParseException, IOException {
		ParseTree tree = parse("test0");
		Result result = check(tree);
		ParseTree assX = tree.getChild(1).getChild(3);
		System.out.println(assX.getText());
		ParseTree assXX = assX.getChild(2);
		System.out.println(assXX.getText());
		assertEquals(Type.INT, result.getType(assX));
		assertEquals(Type.INT, result.getType(assXX.getChild(0)));
		assertEquals(Type.INT, result.getType(assXX.getChild(2)));
	}
	
	@Test
	public void test1() throws ParseException, IOException {
		ParseTree tree = parse("test1");
		System.out.println(tree.toStringTree());
		Result result = check(tree);
		ParseTree a = tree.getChild(1).getChild(1);
		System.out.println(a.getText());
		assertEquals(Type.VOID, result.getType(a));
		assertEquals(Type.INT, result.getType(a.getChild(3)));
		ParseTree print = tree.getChild(1).getChild(7);
		assertEquals(Type.INT, result.getType(print.getChild(2))); 
		assertEquals(Type.INT, result.getType(print));
	}

	@Test
	public void test2() throws ParseException, IOException {
		ParseTree tree = parse("test2");
		System.out.println(tree.toStringTree());
		Result result = check(tree);
	}
	
	@Test
	public void test3() throws ParseException, IOException {
		ParseTree tree = parse("test3");
		System.out.println(tree.toStringTree());
		Result result = check(tree);
	}
	
	private ParseTree parse(String filename) throws IOException, ParseException {
		return this.compiler.parse(new File(BASE_DIR, filename + EXT));
	}

	private Result check(ParseTree tree) throws ParseException {

		return this.compiler.check(tree);
	}
}
