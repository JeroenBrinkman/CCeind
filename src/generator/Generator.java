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

	/**
	 * Helpfunction for TypedStore stores a string
	 */
	private void storeString(ParseTree from, ParseTree to, boolean closeScope, String fromid, String toid) {
		// SETUP
		int[] stringData = mM.getSizeAndOffset(from, fromid);
		if (closeScope) {
			mM.closeScope();
		}
		int parentoff = mM.getOffset(to, stringData[0], toid);
		Reg helpreg = new Reg(mM.getConstReg());

		// MOVE ALL CHARS
		for (int i = 0; i < stringData[0]; i = i + Machine.DEFAULT_CHAR_SIZE) {
			emit(OpCode.cloadAI, arp, new Num(i + stringData[1]), helpreg);
			emit(OpCode.cstoreAI, helpreg, arp, new Num(i + parentoff));
		}
	}

	/**
	 * Store the result of a child ctx as the result of the parent ctx (another
	 * ctx). IF an id is know it will store the value of the id as the result of
	 * the parent.
	 * 
	 * Closes the scope if necesary
	 * 
	 * @requires from != null
	 * @requires to != null
	 */
	private void typedStore(ParseTree from, ParseTree to, boolean closeScope, String id) {
		if (id != null) {
			// hoeft volgens mij niet
		} else if (mM.hasMemory(from)) {
			if (checkResult.getType(from).equals(Type.CHAR)) {
				emit(OpCode.cloadAI, arp, offset(from), reg(to));
				if (closeScope)
					mM.closeScope();
				emit(OpCode.cstoreAI, reg(to), arp, offset(to));
			} else if (checkResult.getType(from).equals(Type.STRING)) {
				this.storeString(from, to, closeScope, null, id);
			} else {
				emit(OpCode.loadAI, arp, offset(from), reg(to));
				if (closeScope)
					mM.closeScope();
				emit(OpCode.storeAI, reg(to), arp, offset(to));
			}
		} else {
			if (checkResult.getType(from).equals(Type.CHAR)) {
				if (closeScope)
					mM.closeScope();
				emit(OpCode.cstoreAI, reg(from), arp, offset(to));
			} else if (checkResult.getType(from).equals(Type.STRING)) {
				System.out.println("this state should not happen");
			} else {
				mM.closeScope();
				emit(OpCode.storeAI, reg(from), arp, offset(to));
			}
		}
	}

	/**
	 * Loads an id from memory for use by the given node
	 * 
	 * @requires id != null
	 * @requires ctx != null
	 */
	private void typedLoad(ParseTree ctx, String id) {
		Type type = checkResult.getType(ctx);
		if (type.equals(Type.CHAR)) {
			emit(OpCode.cloadAI, arp, offset(ctx, id), reg(ctx));
		} else if (type.equals(Type.STRING)) {
			// Strings cant be loaded into a single register
		} else {
			emit(OpCode.loadAI, arp, offset(ctx, id), reg(ctx));
		}
	}

	/**
	 * Improved visit method. Visits the node and if the result of the node was
	 * stored to an id loads that value into the register of the node.
	 * 
	 * @requires ctx != null
	 * @return
	 */
	private String visitH(ParseTree ctx) {
		String id0 = visit(ctx);
		if (id0 != null && mM.hasMemory(ctx)) {
			typedLoad(ctx, id0);
		}
		return id0;
	}

	/**
	 * Generates the necesary iloc code to return the result of one node and
	 * load it as input into the other node. If an id is known it will use that
	 * instead of the node.
	 * 
	 * @requires from != null
	 * @requires to != null
	 */
	private void returnResult(ParseTree from, ParseTree to, String fromid, String toid) {
		Type type = checkResult.getType(from);
		if (mM.hasReg(from) || fromid != null || mM.hasMemory(from)) {
			if (type.equals(Type.CHAR)) {
				emit(OpCode.cstoreAI, reg(from), arp, offset(to, toid));
			} else if (type.equals(Type.STRING)) {
				storeString(from, to, false, fromid, toid);
			} else {
				emit(OpCode.storeAI, reg(from), arp, offset(to, toid));
			}
		} else {
			// No return needed
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

	/**
	 * If the id is not null it will look up the memory offset of this id, or
	 * create a new one if it doesnt exist.
	 * 
	 * If the id is null it will look up the memory offset of the return value
	 * of the node, or create a new one if it doesnt exist.
	 * 
	 * @requires node != null
	 * @ensures result>0
	 */
	private Num offset(ParseTree node, String id) {
		int size = 0;
		if (checkResult.getType(node).equals(Type.INT) || checkResult.getType(node).equals(Type.BOOL)) {
			size = Machine.INT_SIZE;
		} else if (checkResult.getType(node).equals(Type.CHAR)) {
			size = Machine.DEFAULT_CHAR_SIZE;
		}
		Num offset = new Num(mM.getOffset(node, size, id));
		return offset;
	}

	/**
	 * @see offset(node, null);
	 */
	private Num offset(ParseTree node) {
		return offset(node, null);
	}

	/**
	 * Gives the reg that belongs to this node, reserves a new reg if the node
	 * does not have a reg.
	 * 
	 * @requires node != null
	 * @ensure result != null
	 */
	private Reg reg(ParseTree node) {
		Reg reg;
		// IF the value is ONLY stored in memory, it should be loaded into the
		// register before use

		if (mM.hasMemory(node) && !mM.hasReg(node)) {
			reg = new Reg(mM.getNodeReg(node));
			Type type = checkResult.getType(node);
			if (type.equals(Type.CHAR)) {
				emit(OpCode.cloadAI, arp, offset(node), reg);
			} else if (type.equals(Type.STRING)) {
				// Cannot preload for strings

			} else {
				emit(OpCode.loadAI, arp, offset(node), reg);
			}

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

	// -----------Expression-----------

	@Override
	public String visitParExpr(ParExprContext ctx) {
		visitH(ctx.expr());
		emit(OpCode.i2i, reg(ctx.expr()), reg(ctx));
		return null;
	}

	@Override
	public String visitCompExpr(CompExprContext ctx) {
		String id1 = visitH(ctx.expr(0));
		String id2 = visitH(ctx.expr(1));
		if (id1 != null) {
			typedLoad(ctx.expr(0), id1);
		}
		if (id2 != null) {
			typedLoad(ctx.expr(1), id2);
		}
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
		case ("=="):
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
		String id = visitH(ctx.expr(1));
		if (id != null) {
			typedLoad(ctx.expr(1), id);
		}
		returnResult(ctx.expr(1), ctx, id, id);
		emit(OpCode.jumpI, endIf);
		if (elseExists) {
			emit(elsez, OpCode.nop);
			id = visitH(ctx.expr(2));
			if (id != null) {
				typedLoad(ctx.expr(1), id);
			}
			returnResult(ctx.expr(2), ctx, id, id);
		}
		emit(endIf, OpCode.nop);
		return null;
	}

	@Override
	public String visitBlockExpr(BlockExprContext ctx) {
		int last = ctx.expr().size() - 1;
		mM.openScope();
		for (int i = 0; i < ctx.expr().size() - 1; i++) {
			visitH(ctx.expr(i));
		}
		String id = visitH(ctx.expr(last));
		if (id != null) {
			typedLoad(ctx.expr(last), id);
		}
		typedStore(ctx.expr(last), ctx, true, id);
		return null;
	}

	@Override
	public String visitPrintExpr(PrintExprContext ctx) {
		Type type;
		if (ctx.expr().size() > 1) {
			for (int i = 0; i < ctx.expr().size(); i++) {
				String id = visitH(ctx.expr(i));

				if (id != null) {
					typedLoad(ctx.expr(i), id);
				}
				type = checkResult.getType(ctx.expr(i));
				if (type.equals(Type.CHAR)) {
					emit(OpCode.loadI, new Num(1), reg(ctx));
					emit(OpCode.cpush, reg(ctx.expr(i)));
					emit(OpCode.push, reg(ctx));
					emit(OpCode.cout, new Str(ctx.expr(i).getText() + ": "));
				} else if (type.equals(Type.STRING)) {
					int[] stringData = mM.getSizeAndOffset(ctx.expr(i), id);
					// Push chars
					for (int j = stringData[0]; j > 0; j--) {
						emit(OpCode.cloadAI, arp, new Num(stringData[1] + j * Machine.DEFAULT_CHAR_SIZE - 1), reg(ctx));

						emit(OpCode.cpush, reg(ctx));
					}
					// Push size
					emit(OpCode.loadI, new Num(stringData[0]), reg(ctx));
					emit(OpCode.push, reg(ctx));

					emit(OpCode.cout, new Str(ctx.expr(i).getText() + ": "));
				} else {
					emit(OpCode.out, new Str(ctx.expr(i).getText() + ": "), reg(ctx.expr(i)));
					storeString(ctx.expr(0), ctx, false, id, null);
				}
			}
		} else {
			String id = visitH(ctx.expr(0));
			if (id != null) {
				typedLoad(ctx.expr(0), id);
			}
			type = checkResult.getType(ctx.expr(0));
			if (type.equals(Type.CHAR)) {
				emit(OpCode.loadI, new Num(1), reg(ctx));
				emit(OpCode.cpush, reg(ctx.expr(0)));
				emit(OpCode.push, reg(ctx));
				emit(OpCode.cout, new Str(ctx.expr(0).getText() + ": "));
				returnResult(ctx.expr(0), ctx, null, null);
			} else if (type.equals(Type.STRING)) {
				int[] stringData = mM.getSizeAndOffset(ctx.expr(0), id);
				// Push chars
				for (int j = stringData[0]; j > 0; j--) {
					emit(OpCode.cloadAI, arp, new Num(stringData[1] + j * Machine.DEFAULT_CHAR_SIZE - 1), reg(ctx));

					emit(OpCode.cpush, reg(ctx));
				}
				// Push size
				emit(OpCode.loadI, new Num(stringData[0]), reg(ctx));
				emit(OpCode.push, reg(ctx));

				emit(OpCode.cout, new Str(ctx.expr(0).getText() + ": "));

				storeString(ctx.expr(0), ctx, false, id, null);
			} else {
				emit(OpCode.out, new Str(ctx.expr(0).getText() + ": "), reg(ctx.expr(0)));
				returnResult(ctx.expr(0), ctx, null, null);
			}
		}
		return null;
	}

	@Override
	public String visitReadExpr(ReadExprContext ctx) {
		Type[] types = checkResult.getReadTypes(ctx);
		for (int i = 0; i < ctx.ID().size(); i++) {
			if (types[i].equals(Type.CHAR)) {
				emit(OpCode.cin, new Str(ctx.ID(i).getText() + "? : "));
				emit(OpCode.pop, reg(ctx));
				emit(OpCode.cpop, reg(ctx));
				emit(OpCode.cstoreAI, reg(ctx), arp, offset(ctx, ctx.ID(i).getText()));
			} else if (types[i].equals(Type.STRING)) {
				// Not supported by our memory/registry manager, too much
				// work to edit it in properly
			} else {
				emit(OpCode.in, new Str(ctx.ID(i).getText() + "? : "), reg(ctx));
				emit(OpCode.storeAI, reg(ctx), arp, offset(ctx, ctx.ID(i).getText()));
			}
		}

		if (ctx.ID().size() == 1) {
			return ctx.ID(0).getText();
		}
		return null;
	}

	@Override
	public String visitMultExpr(MultExprContext ctx) {
		String id1 = visitH(ctx.expr(0));
		String id2 = visitH(ctx.expr(1));
		if (id1 != null) {
			typedLoad(ctx.expr(0), id1);
		}
		if (id2 != null) {
			typedLoad(ctx.expr(1), id2);
		}
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
		String id1 = visitH(ctx.expr(0));
		String id2 = visitH(ctx.expr(1));
		if (id1 != null) {
			typedLoad(ctx.expr(0), id1);
		}
		if (id2 != null) {
			typedLoad(ctx.expr(1), id2);
		}
		if (checkResult.getType(ctx).equals(Type.INT)) {
			if (ctx.plusOp().getText().equals("+")) {
				emit(OpCode.add, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
			} else {
				emit(OpCode.sub, reg(ctx.expr(0)), reg(ctx.expr(1)), reg(ctx));
			}
		} else {
			// concat string+string or string+char
			int[] stringData1 = mM.getSizeAndOffset(ctx.expr(0), id1);
			int[] stringData2 = mM.getSizeAndOffset(ctx.expr(1), id2);
			int offset = mM.getOffset(ctx, stringData1[0] + stringData2[0], null);
			for (int i = 0; i < stringData1[0]; i += Machine.DEFAULT_CHAR_SIZE, offset += Machine.DEFAULT_CHAR_SIZE) {
				emit(OpCode.cloadAI, arp, new Num(stringData1[1] + i), reg(ctx));
				emit(OpCode.cstoreAI, reg(ctx), arp, new Num(offset));
			}
			for (int i = 0; i < stringData2[0]; i += Machine.DEFAULT_CHAR_SIZE, offset += Machine.DEFAULT_CHAR_SIZE) {
				emit(OpCode.cloadAI, arp, new Num(stringData2[1] + i), reg(ctx));
				emit(OpCode.cstoreAI, reg(ctx), arp, new Num(offset));
			}
		}
		return null;
	}

	@Override
	public String visitPrfExpr(PrfExprContext ctx) {
		String id = visitH(ctx.expr());
		if (id != null) {
			typedLoad(ctx.expr(), id);
		}

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
			Type type = checkResult.getType(ctx);
			if (type.equals(Type.CHAR)) {
				emit(OpCode.cstoreAI, reg(ctx.expr()), arp, offset(ctx.ID(), ctx.ID().getText()));
			} else if (type.equals(Type.STRING)) {
				storeString(ctx.expr(), ctx, false, null, ctx.ID().getText());
			} else {
				emit(OpCode.storeAI, reg(ctx.expr()), arp, offset(ctx.ID(), ctx.ID().getText()));
			}

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
		String id = visitH(ctx.expr());
		if (id != null) {
			typedLoad(ctx.expr(), id);
		}
		if (checkResult.getType(ctx).equals(Type.CHAR)) {
			emit(OpCode.cstoreAI, reg(ctx.expr()), this.arp, offset(ctx.target(), ctx.target().getText()));
		} else if (checkResult.getType(ctx).equals(Type.STRING)) {
			storeString(ctx.expr(), ctx, false, id, ctx.target().getText());
		} else {
			emit(OpCode.storeAI, reg(ctx.expr()), this.arp, offset(ctx.target(), ctx.target().getText()));
		}
		return ctx.target().getText();
	}

	@Override
	public String visitBoolExpr(BoolExprContext ctx) {
		String id1 = visitH(ctx.expr(0));
		String id2 = visitH(ctx.expr(1));
		if (id1 != null) {
			typedLoad(ctx.expr(0), id1);
		}
		if (id2 != null) {
			typedLoad(ctx.expr(1), id2);
		}
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
		return ctx.ID().getText();
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
		emit(OpCode.i2c, reg(ctx), reg(ctx));
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
			emit(OpCode.i2c, reg(ctx), reg(ctx));
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
