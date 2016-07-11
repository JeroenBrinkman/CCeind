package checker;

import grammar.TempNameBaseListener;
import grammar.TempNameParser;
import grammar.TempNameParser.ExprContext;
import grammar.TempNameParser.*;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/** Class to type check and calculate flow entries and variable offsets. */
public class TypeChecker extends TempNameBaseListener {
	/** Result of the latest call of {@link #check}. */
	private Result result;
	/** List of errors collected in the latest call of {@link #check}. */
	private List<String> errors;
	/** Variable scope for the latest call of {@link #check}. */
	private SymbolTable sT;

	/**
	 * Runs this checker on a given parse tree, and returns the checker result.
	 * 
	 * @throws ParseException
	 *             if an error was found during checking.
	 */
	public Result check(ParseTree tree) throws ParseException {
		this.result = new Result();
		this.errors = new ArrayList<>();
		new ParseTreeWalker().walk(this, tree);
		if (hasErrors()) {
			throw new ParseException(getErrors());
		}
		return this.result;
	}

	/** Indicates if any errors were encountered in this tree listener. */
	public boolean hasErrors() {
		return !getErrors().isEmpty();
	}

	/** Returns the list of errors collected in this tree listener. */
	public List<String> getErrors() {
		return this.errors;
	}

	/**
	 * Checks the inferred type of a given parse tree, and adds an error if it
	 * does not correspond to the expected type.
	 */
	private void checkType(ParserRuleContext node, Type expected) {
		Type actual = getType(node);
		if (actual == null) {
			throw new IllegalArgumentException("Missing inferred type of " + node.getText());
		}
		if (!actual.equals(expected)) {
			addError(node, "Expected type '%s' but found '%s'", expected, actual);
		}
	}

	/**
	 * Checks the inferred type of a given parse tree, and adds an error if it
	 * does correspond to the expected type.
	 */
	private void checkNotType(ParserRuleContext node, Type expected) {
		Type actual = getType(node);
		if (actual == null) {
			throw new IllegalArgumentException("Missing inferred type of " + node.getText());
		}
		if (actual.equals(expected)) {
			addError(node, "Illegal type '%s' not allowed", expected);
		}
	}

	/**
	 * Records an error at a given parse tree node.
	 * 
	 * @param ctx
	 *            the parse tree node at which the error occurred
	 * @param message
	 *            the error message
	 * @param args
	 *            arguments for the message, see {@link String#format}
	 */
	public void addError(ParserRuleContext node, String message, Object... args) {
		addError(node.getStart(), message, args);
	}

	/**
	 * Records an error at a given token.
	 * 
	 * @param token
	 *            the token at which the error occurred
	 * @param message
	 *            the error message
	 * @param args
	 *            arguments for the message, see {@link String#format}
	 */
	private void addError(Token token, String message, Object... args) {
		int line = token.getLine();
		int column = token.getCharPositionInLine();
		message = String.format(message, args);
		message = String.format("Line %d:%d - %s", line, column, message);
		this.errors.add(message);
	}

	/** Convenience method to add a type to the result. */
	private void setType(ParseTree node, Type type) {
		this.result.setType(node, type);
	}

	/** Returns the type of a given expression or type node. */
	private Type getType(ParseTree node) {
		return this.result.getType(node);
	}

	/** Convenience method to add a flow graph entry to the result. */
	private void setEntry(ParseTree node, ParserRuleContext entry) {
		if (entry == null) {
			throw new IllegalArgumentException("Null flow graph entry");
		}
		this.result.setEntry(node, entry);
	}

	/** Returns the flow graph entry of a given expression or statement. */
	private ParserRuleContext entry(ParseTree node) {
		return this.result.getEntry(node);
	}

	@Override
	public void enterProgram(@NotNull TempNameParser.ProgramContext ctx) {
		sT = new SymbolTable();
		sT.openScope();
	}

	@Override
	public void exitProgram(@NotNull TempNameParser.ProgramContext ctx) {
		sT.closeScope();
	}

	@Override
	public void enterBlockExpr(BlockExprContext ctx) {
		sT.openScope();
	}

	@Override
	public void exitTrueExpr(TrueExprContext ctx) {
		setType(ctx, Type.BOOL);
		setEntry(ctx, ctx);
	}

	@Override
	public void exitBoolType(BoolTypeContext ctx) {
		setType(ctx, Type.BOOL);
	}

	@Override
	public void exitStringExpr(StringExprContext ctx) {
		setType(ctx, Type.STRING);
		setEntry(ctx, ctx);
	}

	@Override
	public void exitParExpr(ParExprContext ctx) {
		setType(ctx, getType(ctx.expr()));
		setEntry(ctx, entry(ctx.expr()));
	}

	@Override
	public void exitCompExpr(CompExprContext ctx) {
		if (ctx.compOp().getText().equals("<>") || ctx.compOp().getText().equals("==")) {
			checkType(ctx.expr(1), getType(ctx.expr(0)));
		} else {
			checkType(ctx.expr(0), Type.INT);
			checkType(ctx.expr(1), Type.INT);
		}
		setType(ctx, Type.BOOL);
		setEntry(ctx, entry(ctx.expr(0)));
	}

	@Override
	public void exitStringType(StringTypeContext ctx) {
		setType(ctx, Type.STRING);
	}

	@Override
	public void exitIfExpr(IfExprContext ctx) {
		checkType(ctx.expr(0), Type.BOOL);
		setEntry(ctx, entry(ctx.expr(0)));
		if (ctx.expr(2) != null) {
			checkType(ctx.expr(1), getType(ctx.expr(2)));
		}
		setType(ctx, getType(ctx.expr(1)));
	}

	@Override
	public void exitBlockExpr(BlockExprContext ctx) {
		ExprContext last = ctx.expr(ctx.expr().size() - 1);
		setType(ctx, getType(last));
		if (ctx.expr().size() > 0) {
			setEntry(ctx, entry(ctx.expr(0)));
		}
		sT.closeScope();
	}

	@Override
	public void exitFalseExpr(FalseExprContext ctx) {
		setType(ctx, Type.BOOL);
		setEntry(ctx, ctx);
	}

	@Override
	public void exitPrintExpr(PrintExprContext ctx) {
		for (ExprContext e : ctx.expr()) {
			checkNotType(e, Type.VOID);
		}
		if (ctx.expr().size() == 1) {
			setType(ctx, getType(ctx.expr(0)));
			setEntry(ctx, entry(ctx.expr(0)));
		} else {
			setEntry(ctx, entry(ctx.expr(0)));
			setType(ctx, Type.VOID);
		}
	}

	@Override
	public void exitCharType(CharTypeContext ctx) {
		setType(ctx, Type.CHAR);
	}

	@Override
	public void exitIdTarget(IdTargetContext ctx) {
		final String id = ctx.ID().getText();
		if (!sT.contains(id)) {
			addError(ctx, "Variable '%s' not declared", id);
			setEntry(ctx, ctx);
			setType(ctx, Type.VOID);
		} else {
			setType(ctx, sT.getType(id));
			setEntry(ctx, ctx);
			// setOffset(ctx, sT.offset(id));
		}
	}

	@Override
	public void exitCharExpr(CharExprContext ctx) {
		setType(ctx, Type.CHAR);
		setEntry(ctx, ctx);
	}

	@Override
	public void exitIntType(IntTypeContext ctx) {
		setType(ctx, Type.INT);
	}

	@Override
	public void exitReadExpr(ReadExprContext ctx) {
		Type[] tps = new Type[ctx.ID().size()];
		for (int i = 0; i < ctx.ID().size(); i++) {
			String targetString = ctx.ID(i).toString();
			if (!sT.contains(targetString)) {
				errors.add(" Attempting to read for a undeclared variable '" + ctx.ID().toString() + "' !");
			}
			tps[i] = sT.getType(ctx.ID(i).getText());
		}

		result.setReadTypes(ctx, tps);
		if (ctx.ID().size() > 1) {
			setType(ctx, Type.VOID);
			setEntry(ctx, ctx);
		} else {
			setType(ctx, sT.getType(ctx.ID(0).getText()));
			setEntry(ctx, ctx);
		}

	}

	@Override
	public void exitMultExpr(MultExprContext ctx) {
		checkType(ctx.expr(0), Type.INT);
		checkType(ctx.expr(1), Type.INT);
		setType(ctx, Type.INT);
		setEntry(ctx, entry(ctx.expr(0)));
	}

	@Override
	public void exitNumExpr(NumExprContext ctx) {
		setType(ctx, Type.INT);
		setEntry(ctx, ctx);
	}

	@Override
	public void exitPlusExpr(PlusExprContext ctx) {
		if (getType(ctx.expr(0)).equals(Type.STRING)) {
			if (getType(ctx.expr(0)).equals(Type.STRING) || getType(ctx.expr(0)).equals(Type.CHAR)) {
				setType(ctx, Type.STRING);
			}else{
				addError(ctx, "Illegal type second argument, STRING or CHAR expected.");
				setType(ctx, Type.VOID);
			}
		} else if (getType(ctx.expr(0)).equals(Type.INT)) {
			checkType(ctx.expr(1), Type.INT);
			setType(ctx, Type.INT);
		} else {
			addError(ctx, "Illegal type first argument, STRING or INT expected.");
			setType(ctx, Type.VOID);
		}
		setEntry(ctx, entry(ctx.expr(0)));
	}

	@Override
	public void exitDeclExpr(DeclExprContext ctx) {
		if (ctx.expr() != null) {
			checkType(ctx.expr(), getType(ctx.type()));
			setEntry(ctx, entry(ctx.expr()));
			if (!sT.add(ctx.ID().getText(), getType(ctx.type()))) {
				addError(ctx, "Illegal declaration, '$s' already in scope.", ctx.ID().getText());
			} else {

				setType(ctx.ID(), getType(ctx.type()));
				setType(ctx, getType(ctx.type()));
				return;
			}
		} else {
			if (!sT.add(ctx.ID().getText(), getType(ctx.type()))) {
				addError(ctx, "Illegal declaration, '$s' already in scope.", ctx.ID().getText());

			}
			// TODO entry?
		}
		setType(ctx, Type.VOID);
	}

	@Override
	public void exitWhileExpr(WhileExprContext ctx) {
		checkType(ctx.expr(0), Type.BOOL);
		setEntry(ctx, entry(ctx.expr(0)));
		setType(ctx, Type.VOID);
	}

	@Override
	public void exitAssExpr(AssExprContext ctx) {
		checkType(ctx.expr(), getType(ctx.target()));
		setType(ctx, getType(ctx.target()));
		setEntry(ctx, entry(ctx.expr()));
	}

	@Override
	public void exitPrfExpr(PrfExprContext ctx) {
		Type type;
		if (ctx.prfOp().MINUS() != null) {
			type = Type.INT;
		} else {
			type = Type.BOOL;
		}
		checkType(ctx.expr(), type);
		setType(ctx, type);
		setEntry(ctx, entry(ctx.expr()));
	}

	@Override
	public void exitBoolExpr(BoolExprContext ctx) {
		checkType(ctx.expr(0), Type.BOOL);
		checkType(ctx.expr(1), Type.BOOL);
		setType(ctx, Type.BOOL);
		setEntry(ctx, entry(ctx.expr(0)));
	}

	@Override
	public void exitIdExpr(IdExprContext ctx) {
		String id = ctx.ID().getText();
		Type type = this.sT.getType(id);
		if (type == null) {
			addError(ctx, "Variable '%s' not declared", id);
			setType(ctx, Type.VOID);
		} else {
			setType(ctx, type);
			// setOffset(ctx, this.scope.offset(id));
			setEntry(ctx, ctx);
		}
	}

}