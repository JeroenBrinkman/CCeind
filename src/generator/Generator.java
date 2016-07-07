package generator;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import iloc.eval.Machine;
import iloc.model.*;
import iloc.Simulator;
import checker.Result;
import checker.Type;
import grammar.TempNameBaseVisitor;
import grammar.TempNameParser.*;

public class Generator extends TempNameBaseVisitor<String> {
	/** The representation of the boolean value <code>false</code>. */
	public final static Num FALSE_VALUE = new Num(Simulator.FALSE);
	/** The representation of the boolean value <code>true</code>. */
	public final static Num TRUE_VALUE = new Num(Simulator.TRUE);

	/** The base register. */
	private Reg arp = new Reg("r_arp");
	/** The outcome of the checker phase. */
	private Result checkResult;
	/** Association of statement nodes to labels. */
	private ParseTreeProperty<Label> labels;
	/** The program being built. */
	private Program prog;
	/** The memory manager of this generator */
	private MemoryManager mM;

	/**
	 * Generates ILOC code for a given parse tree, given a pre-computed checker
	 * result.
	 */
	public Program generate(ParseTree tree, Result checkResult) {
		this.prog = new Program();
		this.checkResult = checkResult;
		this.labels = new ParseTreeProperty<>();
		this.mM = new MemoryManager();
		tree.accept(this);
		return this.prog;
	}

	private void moveString(ParseTree from, ParseTree to, boolean closeScope) {
		// SETUP
		int[] stringData = mM.getSizeAndOffset(from);
		if (closeScope) {
			mM.closeScope();
		}
		int parentoff = mM.getOffset(to, stringData[0], null);
		Reg helpreg = new Reg(mM.getConstReg());

		// MOVE ALL CHARS
		for (int i = 0; i < stringData[1]; i = i + Machine.DEFAULT_CHAR_SIZE) {
			emit(OpCode.cloadAI, arp, new Num(i), helpreg);
			emit(OpCode.cstoreAI, helpreg, arp, new Num(i + parentoff));
		}
	}
	
	private String visitH(ParseTree ctx){
		String id0 = visit(ctx);
		if(id0 != null){
			emit(OpCode.loadAI, arp, offset(ctx, id0), reg(ctx));
		}
		return id0;
	}

	private void returnResult(ParseTree child, ParseTree parent, String id) {
		Type type = checkResult.getType(child);
		if (mM.hasReg(child)) {
			if (type.equals(Type.CHAR)) {
				emit(OpCode.cstoreAI, reg(child), arp, offset(parent));
			} else if (type.equals(Type.STRING)) {
				// TODO is this case illegal?
				moveString(child, parent, false);
			} else {
				emit(OpCode.storeAI, reg(child), arp, offset(parent));
			}
		} else if (mM.hasMemory(child)) {
			if (type.equals(Type.CHAR)) {
				emit(OpCode.cloadAI, arp, offset(child), reg(child));
				emit(OpCode.cstoreAI, reg(child), arp, offset(parent));
			} else if (type.equals(Type.STRING)) {
				moveString(child, parent, false);
			} else {
				emit(OpCode.loadAI, arp, offset(child), reg(child));
				emit(OpCode.storeAI, reg(child), arp, offset(parent));
			}
		} else if (id != null) {
			if (type.equals(Type.CHAR)) {
				emit(OpCode.cloadAI, arp, offset(child, id), reg(parent));
				emit(OpCode.cstoreAI, reg(parent), arp, offset(parent));
			} else if (type.equals(Type.STRING)) {
				moveString(child, parent, false);
			} else {
				emit(OpCode.loadAI, arp, offset(child, id), reg(parent));
				emit(OpCode.storeAI, reg(parent), arp, offset(parent));
			}
		} else {
			// TODO error, illegal state
		}
	}

	/**
	 * Constructs an operation from the parameters and adds it to the program
	 * under construction.
	 */
	private Op emit(Label label, OpCode opCode, Operand... args) {
		Op result = new Op(label, opCode, args);
		this.prog.addInstr(result);
		return result;
	}

	/**
	 * Constructs an operation from the parameters and adds it to the program
	 * under construction.
	 */
	private Op emit(OpCode opCode, Operand... args) {
		return emit((Label) null, opCode, args);
	}

	/**
	 * Looks up the label for a given parse tree node, creating it if none has
	 * been created before. The label is actually constructed from the entry
	 * node in the flow graph, as stored in the checker result.
	 */
	private Label label(ParserRuleContext node) {
		Label result = this.labels.get(node);
		if (result == null) {
			ParserRuleContext entry = this.checkResult.getEntry(node);
			result = createLabel(entry, "n");
			this.labels.put(node, result);
		}
		return result;
	}

	/** Creates a label for a given parse tree node and prefix. */
	private Label createLabel(ParserRuleContext node, String prefix) {
		Token token = node.getStart();
		int line = token.getLine();
		int column = token.getCharPositionInLine();
		String result = prefix + "_" + line + "_" + column;
		return new Label(result);
	}

	private Num offset(ParseTree node, String id) {
		int size = 0;
		if (checkResult.getType(node).equals(Type.INT)) {
			size = Machine.INT_SIZE;
		} else if (checkResult.getType(node).equals(Type.CHAR)) {
			size = Machine.DEFAULT_CHAR_SIZE;
		}
		Num offset = new Num(mM.getOffset(node, size, id));
		return offset;
	}

	private Num offset(ParseTree node) {
		int size = 0;
		if (checkResult.getType(node).equals(Type.INT)) {
			size = Machine.INT_SIZE;
		} else if (checkResult.getType(node).equals(Type.CHAR)) {
			size = Machine.DEFAULT_CHAR_SIZE;
		}
		Num offset = new Num(mM.getOffset(node, size, null));
		return offset;
	}

	private Reg reg(ParseTree node) {
		Reg reg;
		// IF the value is ONLY stored in memory, it should be loaded into the
		// register before use

		if (mM.hasMemory(node) && !mM.hasReg(node)) {
			System.out.println("asfdasfd");
			reg = new Reg(mM.getNodeReg(node));
			Type elseType = checkResult.getType(node);
			if (elseType.equals(Type.CHAR)) {
				emit(OpCode.cloadAI, arp, offset(node), reg);
			} else {
				emit(OpCode.loadAI, arp, offset(node), reg);
			} // TODO extend for booleans, Strings } else {
		} else {
			reg = new Reg(mM.getNodeReg(node));
		}

		return reg;
	}

	// -----------Program-----------

	@Override
	public String visitProgram(ProgramContext ctx) {
		emit(new Label("Program"), OpCode.nop);
		mM.openScope();
		visit(ctx.expr());
		mM.closeScope();
		return null;
	}

	// -----------Target-----------

	@Override
	public String visitIdTarget(IdTargetContext ctx) {
		emit(OpCode.storeAI, reg(ctx), arp, offset(ctx, ctx.ID().getText()));
		return ctx.ID().getText();
	}

	// -----------Expression-----------

	@Override
	public String visitParExpr(ParExprContext ctx) {
		visitH(ctx.expr());
		emit(OpCode.i2i, reg(ctx.expr()), reg(ctx));
		return null;
	}

	@Override
	public String visitCompExpr(CompExprContext ctx) {
		visitH(ctx.expr(0));
		visitH(ctx.expr(1));
		String compOp = ctx.compOp().getText();
		switch (compOp) {
		case ("<="):
			emit(OpCode.cmp_LE, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
			break;
		case ("<"):
			emit(OpCode.cmp_LT, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
			break;
		case ("<>"):
			emit(OpCode.cmp_NE, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
			break;
		case (">="):
			emit(OpCode.cmp_GE, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
			break;
		case (">"):
			emit(OpCode.cmp_GT, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
			break;
		case ("="):
			emit(OpCode.cmp_EQ, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
			break;
		}

		return null;
	}

	@Override
	public String visitIfExpr(IfExprContext ctx) {
		visitH(ctx.expr(0));
		Label endIf = createLabel(ctx, "endIf");

		boolean elseExists = ctx.expr(2) != null;
		Label elsez = elseExists ? label(ctx.expr(2)) : endIf;
		emit(OpCode.cbr, reg(ctx.expr(0)), label(ctx.expr(1)), elsez);
		emit(label(ctx.expr(1)), OpCode.nop);

		String id = visit(ctx.expr(1));
		returnResult(ctx.expr(1), ctx, id);
		emit(OpCode.jumpI, endIf);
		if (elseExists) {
			emit(elsez, OpCode.nop);
			id = visit(ctx.expr(2));

			returnResult(ctx.expr(2), ctx, id);
		}
		emit(endIf, OpCode.nop);
		return null;
	}

	@Override
	public String visitBlockExpr(BlockExprContext ctx) {
		int last = ctx.expr().size() - 1;
		mM.openScope();
		for (int i = 0; i < ctx.expr().size() - 1; i++) {
			visit(ctx.expr(i));
		}
		visit(ctx.expr(last));
		if (checkResult.getType(ctx.expr(last)).equals(Type.CHAR)) {
			emit(OpCode.cloadAI, arp, offset(ctx.expr(last)), reg(ctx));
			mM.closeScope();
			emit(OpCode.cstoreAI, reg(ctx), arp, offset(ctx));
		} else if (checkResult.getType(ctx.expr(last)).equals(Type.STRING)) {
			this.moveString(ctx.expr(last), ctx, true);
		} else {
			emit(OpCode.loadAI, arp, offset(ctx.expr(last)), reg(ctx));
			mM.closeScope();
			emit(OpCode.storeAI, reg(ctx), arp, offset(ctx));
		}
		return null;
	}

	@Override
	public String visitPrintExpr(PrintExprContext ctx) {
		if (ctx.expr().size() > 1) {
			for (int i = 0; i < ctx.expr().size(); i++) {
				visitH(ctx.expr(i));
				emit(OpCode.out, new Str(ctx.expr(i).getText() + ": "), reg(ctx.expr(i)));
			}
		} else {
			String id = visit(ctx.expr(0));
			emit(OpCode.out, new Str(ctx.expr(0).getText() + ": "), reg(ctx.expr(0)));
			returnResult(ctx.expr(0), ctx, id);
		}
		return null;
	}

	@Override
	public String visitReadExpr(ReadExprContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String visitMultExpr(MultExprContext ctx) {
		visitH(ctx.expr(0));
		visitH(ctx.expr(1));
		if (ctx.multOp().getText().equals("*")) {
			// Times
			emit(OpCode.mult, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
		} else if (ctx.multOp().getText().equals("/")) {
			// Division
			emit(OpCode.div, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
		} else {
			// Modulo
			String str1 = mM.getConstReg();
			Reg r1 = new Reg(str1);
			emit(OpCode.div, reg(ctx.expr(0)), reg(ctx.expr(1)), r1);
			emit(OpCode.mult, r1, reg(ctx.expr(1)), r1);
			emit(OpCode.sub, reg(ctx.expr(0)), r1, reg(ctx));
		}
		return null;
	}

	@Override
	public String visitPlusExpr(PlusExprContext ctx) {
		visitH(ctx.expr(0));
		visitH(ctx.expr(1));
	
		
		if (ctx.plusOp().getText().equals("+")) {
			emit(OpCode.add, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
		} else {
			emit(OpCode.sub, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
		}
		return null;
	}

	@Override
	public String visitPrfExpr(PrfExprContext ctx) {
		visitH(ctx.expr());

		if (ctx.prfOp().getText().equals("-")) {
			emit(OpCode.rsubI, reg(ctx.expr()), new Num(0), reg(ctx));
		} else {
			emit(OpCode.addI, reg(ctx.expr()), new Num(1), reg(ctx));
			emit(OpCode.rsubI, reg(ctx), new Num(0), reg(ctx));
		}
		return null;
	}

	@Override
	public String visitDeclExpr(DeclExprContext ctx) {
		if (ctx.expr() != null) {
			visitH(ctx.expr());


			emit(OpCode.storeAI, reg(ctx.expr()), arp, offset(ctx.ID(), ctx.ID().getText()));
		}
		return ctx.ID().getText();
	}

	@Override
	public String visitWhileExpr(WhileExprContext ctx) {
		emit(label(ctx), OpCode.nop);
		visitH(ctx.expr(0));
		Label endLabel = createLabel(ctx, "end");
		emit(OpCode.cbr, reg(ctx.expr(0)), label(ctx.expr(1)), endLabel);
		emit(label(ctx.expr(1)), OpCode.nop);
		visitH(ctx.expr(1));
		emit(OpCode.jumpI, label(ctx));
		emit(endLabel, OpCode.nop);
		return null;
	}

	@Override
	public String visitAssExpr(AssExprContext ctx) {
		visitH(ctx.expr());
		if (checkResult.getType(ctx).equals(Type.CHAR)) {
			emit(OpCode.cstoreAI, reg(ctx.expr()), this.arp, offset(ctx.target(), ctx.target().getText()));
		} else if (checkResult.getType(ctx).equals(Type.STRING)) {
			moveString(ctx.expr(), ctx, false);
		} else {
			emit(OpCode.storeAI, reg(ctx.expr()), this.arp, offset(ctx.target(), ctx.target().getText()));
		}
		return ctx.target().getText();
	}

	@Override
	public String visitBoolExpr(BoolExprContext ctx) {
		visitH(ctx.expr(0));
		visitH(ctx.expr(1));
		if (ctx.boolOp().getText().contains("o") || ctx.boolOp().getText().contains("O")) {
			emit(OpCode.or, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
		} else {
			emit(OpCode.and, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));

		}
		return null;
	}

	// -----------Terminal expressions-----------

	@Override
	public String visitIdExpr(IdExprContext ctx) {
		emit(OpCode.loadAI, this.arp, offset(ctx, ctx.ID().getText()), reg(ctx));
		return null;
	}

	@Override
	public String visitNumExpr(NumExprContext ctx) {
		emit(OpCode.loadI, new Num(Integer.parseInt(ctx.NUM().getText())), reg(ctx));
		return null;
	}

	@Override
	public String visitCharExpr(CharExprContext ctx) {
		int chara = (int) ctx.CHR().getText().charAt(1);
		emit(OpCode.loadI, new Num(chara), reg(ctx));
		return null;
	}

	@Override
	public String visitStringExpr(StringExprContext ctx) {
		String str = ctx.STR().getText();
		str = str.substring(1, str.length() - 1);
		int offset = mM.getOffset(ctx, Machine.DEFAULT_CHAR_SIZE * str.length(), null);
		for (int i = 0; i < str.length(); i++) {
			int chara = (int) str.charAt(i);
			emit(OpCode.loadI, new Num(chara), reg(ctx));
			emit(OpCode.cstoreAI, reg(ctx), arp, new Num(offset + i * Machine.DEFAULT_CHAR_SIZE));
		}
		return null;
	}

	@Override
	public String visitTrueExpr(TrueExprContext ctx) {
		emit(OpCode.loadI, TRUE_VALUE, reg(ctx));
		return null;
	}

	@Override
	public String visitFalseExpr(FalseExprContext ctx) {
		emit(OpCode.loadI, FALSE_VALUE, reg(ctx));
		return null;
	}
}
