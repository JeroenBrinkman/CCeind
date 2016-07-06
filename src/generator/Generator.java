package generator;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import iloc.model.Op;
import iloc.model.OpCode;
import iloc.model.Operand;
import checker.Result;
import grammar.TempNameBaseVisitor;
import grammar.TempNameParser.AssExprContext;
import grammar.TempNameParser.BlockExprContext;
import grammar.TempNameParser.BoolExprContext;
import grammar.TempNameParser.CharExprContext;
import grammar.TempNameParser.CompExprContext;
import grammar.TempNameParser.DeclExprContext;
import grammar.TempNameParser.FalseExprContext;
import grammar.TempNameParser.IdExprContext;
import grammar.TempNameParser.IdTargetContext;
import grammar.TempNameParser.IfExprContext;
import grammar.TempNameParser.MultExprContext;
import grammar.TempNameParser.NumExprContext;
import grammar.TempNameParser.ParExprContext;
import grammar.TempNameParser.PlusExprContext;
import grammar.TempNameParser.PrfExprContext;
import grammar.TempNameParser.PrintExprContext;
import grammar.TempNameParser.ProgramContext;
import grammar.TempNameParser.ReadExprContext;
import grammar.TempNameParser.StringExprContext;
import grammar.TempNameParser.TrueExprContext;
import grammar.TempNameParser.WhileExprContext;
import iloc.Simulator;
import iloc.model.Label;
import iloc.model.Num;
import iloc.model.Program;
import iloc.model.Reg;

public class Generator extends TempNameBaseVisitor<Op> {
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
	/** Register count, used to generate fresh registers. */
	private int regCount;
	/** Association of expression and target nodes to registers. */
	private ParseTreeProperty<Reg> regs;
	/***/
	private MemoryManager mM;

	/**
	 * Generates ILOC code for a given parse tree, given a pre-computed checker
	 * result.
	 */
	public Program generate(ParseTree tree, Result checkResult) {
		this.prog = new Program();
		this.checkResult = checkResult;
		this.regs = new ParseTreeProperty<>();
		this.labels = new ParseTreeProperty<>();
		this.mM = new MemoryManager();
		this.regCount = 0;
		tree.accept(this);
		return this.prog;
	}

	// Override the visitor methods
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

	/** Assigns a register to a given parse tree node. */
	private void setReg(ParseTree node, Reg reg) {
		// TODO
	}
	
	private Num offset(ParseTree node) {
		//TODO get offset with node
		return null;
		
	}
	private Reg reg(ParseTree node) {
		//TODO get reg with node
		return null;
		
	}
	
	// -----------Program-----------
	
	@Override
	public Op visitProgram(ProgramContext ctx) {
		emit(new Label("Program"), OpCode.nop);
		mM.openScope();
		visit(ctx.expr());
		mM.closeScope();
		return null;
	}
	
	// -----------Target-----------
			
	@Override
	public Op visitIdTarget(IdTargetContext ctx) {
		emit(OpCode.storeAI, reg(ctx), this.arp, offset(ctx));
		return null;
	}
	
	// -----------Expression-----------

	@Override
	public Op visitParExpr(ParExprContext ctx) {
		visit(ctx.expr());
		emit(OpCode.i2i, reg(ctx.expr()), reg(ctx));
		return null;
	}

	@Override
	public Op visitCompExpr(CompExprContext ctx) {
		visit(ctx.expr(0));
		visit(ctx.expr(1));
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
	public Op visitIfExpr(IfExprContext ctx) {
		visit(ctx.expr(0));
		Label endIf = createLabel(ctx, "endIf");

		boolean elseExists = ctx.expr(2) != null;
		Label elsez = elseExists ? label(ctx.expr(2)) : endIf;
		emit(OpCode.cbr, reg(ctx.expr(0)), label(ctx.expr(1)), elsez);
		emit(label(ctx.expr(1)), OpCode.nop);
		visit(ctx.expr(1));
		emit(OpCode.jumpI, endIf);
		if (elseExists) {
			emit(elsez, OpCode.nop);
			visit(ctx.expr(2));
		}
		emit(endIf, OpCode.nop);
		return null;
	}

	@Override
	public Op visitBlockExpr(BlockExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitBlockExpr(ctx);
	}

	@Override
	public Op visitPrintExpr(PrintExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitPrintExpr(ctx);
	}

	@Override
	public Op visitReadExpr(ReadExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitReadExpr(ctx);
	}

	@Override
	public Op visitMultExpr(MultExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitMultExpr(ctx);
	}
	
	@Override
	public Op visitPlusExpr(PlusExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitPlusExpr(ctx);
	}

	@Override
	public Op visitDeclExpr(DeclExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitDeclExpr(ctx);
	}

	@Override
	public Op visitWhileExpr(WhileExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitWhileExpr(ctx);
	}

	@Override
	public Op visitAssExpr(AssExprContext ctx) {
		visit(ctx.expr());
		emit(OpCode.storeAI, reg(ctx.expr()), this.arp, offset(ctx.target()));
		return null;
	}

	@Override
	public Op visitPrfExpr(PrfExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitPrfExpr(ctx);
	}

	@Override
	public Op visitBoolExpr(BoolExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitBoolExpr(ctx);
	}
	
	//-----------Terminal expressions-----------
	
	@Override
	public Op visitIdExpr(IdExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitIdExpr(ctx);
	}

	@Override
	public Op visitNumExpr(NumExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitNumExpr(ctx);
	}
	@Override
	public Op visitCharExpr(CharExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitCharExpr(ctx);
	}

	@Override
	public Op visitStringExpr(StringExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitStringExpr(ctx);
	}
	
	@Override
	public Op visitTrueExpr(TrueExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitTrueExpr(ctx);
	}
	
	@Override
	public Op visitFalseExpr(FalseExprContext ctx) {
		// TODO Auto-generated method stub
		return super.visitFalseExpr(ctx);
	}
}
