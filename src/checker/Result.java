package checker;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

/** Class holding the results of the TempName checker. */
public class Result {
	/**
	 * Mapping from statements and expressions to the atomic subtree that is
	 * their entry in the control flow graph.
	 */
	private final ParseTreeProperty<ParserRuleContext> entries = new ParseTreeProperty<>();
	/** Mapping from expressions to types. */
	private final ParseTreeProperty<Type> types = new ParseTreeProperty<>();
	private final ParseTreeProperty<Type[]> readtypes = new ParseTreeProperty<>();

	/** Adds an association from parse tree node to the flow graph entry. */
	public void setEntry(ParseTree node, ParserRuleContext entry) {
		this.entries.put(node, entry);
	}

	/**
	 * Returns the flow graph entry associated with a given parse tree node.
	 */
	public ParserRuleContext getEntry(ParseTree node) {
		return this.entries.get(node);
	}

	/**
	 * Adds an association from a parse tree expression, type, or assignment
	 * target node to the corresponding (inferred) type.
	 */
	public void setType(ParseTree node, Type type) {
		this.types.put(node, type);
	}

	/** Returns the type associated with a given parse tree node. */
	public Type getType(ParseTree node) {
		return this.types.get(node);
	}

	public void setReadTypes(ParseTree node, Type[] tps) {
		this.readtypes.put(node, tps);
	}

	public Type[] getReadTypes(ParseTree node) {
		return this.readtypes.get(node);
	}
}
