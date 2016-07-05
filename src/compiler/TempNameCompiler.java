package compiler;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import checker.ErrorListener;
import checker.ParseException;
import checker.Result;
import checker.TypeChecker;
import grammar.TempNameLexer;
import grammar.TempNameParser;

public class TempNameCompiler {
	/** The singleton instance of this class. */
	private final static TempNameCompiler instance = new TempNameCompiler();
	/** Debug flag. */
	private final static boolean SHOW = true;

	/** Returns the singleton instance of this class. */
	public static TempNameCompiler instance() {
		return instance;
	}

	/** Compiles and runs the program named in the argument. */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: filename");
			return;
		}
		//try {
			System.out.println("--- Running " + args[0]);
			//Program prog = instance().compile(new File(args[0]));
			if (SHOW) {
				//System.out.println(prog.prettyPrint());
			}
			//Simulator sim = new Simulator(prog);
			//sim.run();
			System.out.println("--- Done with " + args[0]);
		/**} catch (ParseException exc) {
			exc.print();
		} catch (IOException exc) {
			exc.printStackTrace();
		}*/
	}

	/** The fixed checker of this compiler. */
	private final TypeChecker checker;
	/** The fixed generator of this compiler. */
	//private final Generator generator;

	private TempNameCompiler() {
		this.checker = new TypeChecker();
		//this.generator = new Generator();
	}

	/** Typechecks a given Simple Pascal string. */
	public Result check(String text) throws ParseException {
		return check(parse(text));
	}

	/** Typechecks a given Simple Pascal file. */
	public Result check(File file) throws ParseException, IOException {
		return check(parse(file));
	}

	/** Typechecks a given Simple Pascal parse tree. */
	public Result check(ParseTree tree) throws ParseException {
		return this.checker.check(tree);
	}

	/** Compiles a given Simple Pascal string into an ILOC program. 
	public Program compile(String text) throws ParseException {
		return compile(parse(text));
	}*/

	/** Compiles a given Simple Pascal file into an ILOC program. 
	public Program compile(File file) throws ParseException, IOException {
		return compile(parse(file));
	}*/

	/** Compiles a given Simple Pascal parse tree into an ILOC program. 
	public Program compile(ParseTree tree) throws ParseException {
		Result checkResult = this.checker.check(tree);
		
		return this.generator.generate(tree, checkResult);
	}*/

	/** Compiles a given Simple Pascal string into a parse tree. */
	public ParseTree parse(String text) throws ParseException {
		return parse(new ANTLRInputStream(text));
	}

	/** Compiles a given Simple Pascal string into a parse tree. */
	public ParseTree parse(File file) throws ParseException, IOException {
		return parse(new ANTLRInputStream(new FileReader(file)));
	}

	private ParseTree parse(CharStream chars) throws ParseException {
		ErrorListener listener = new ErrorListener();
		Lexer lexer = new TempNameLexer(chars);
		lexer.removeErrorListeners();
		lexer.addErrorListener(listener);
		TokenStream tokens = new CommonTokenStream(lexer);
		TempNameParser parser = new TempNameParser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(listener);
		ParseTree result = parser.program();
		System.out.println(result.toString());
		listener.throwException();
		return result;
	}
}
