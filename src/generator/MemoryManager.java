package generator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.Iterator;

/**
 * Manages memory allocation for the generator/compiler. Register management is
 * forwarded to the register manager.
 * 
 * @author Jeroen
 *
 */
public class MemoryManager {

	/**
	 * Memory block abstraction, Types are irrelevant for the generator.
	 *
	 */
	private class Block implements Comparable<Block> {
		public int start;
		public int size;
		public ParseTree ctx;

		public Block(int st, int si, ParseTree ctx) {
			start = st;
			size = si;
			this.ctx = ctx;
			;
		}

		@Override
		public int compareTo(Block arg0) {
			return this.start - ((Block) arg0).start;
		}
	}

	private final Deque<ArrayList<ParseTree>> varScopes = new ArrayDeque<ArrayList<ParseTree>>();
	private final Deque<ArrayList<String>> conScopes = new ArrayDeque<ArrayList<String>>();
	private final ArrayList<Block> memory;
	private final RegisterManager regman;

	public MemoryManager() {
		memory = new ArrayList<Block>();
		regman = new RegisterManager();
	}

	private boolean inScope(ParseTree ctx) {
		ArrayList<ParseTree> scope;
		for (Iterator<ArrayList<ParseTree>> itr = varScopes.iterator(); itr.hasNext();) {
			scope = itr.next();
			if (scope.contains(ctx)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * reserves a memory block for the given id at the given size. When the size
	 * changes it should perform a free and new reserve action. Returns the
	 * offset at which this id can write to memory
	 */
	public int reserveMemory(ParseTree ctx, int size) {
		// add to scope if not already contained
		if (!inScope(ctx)) {
			varScopes.getFirst().add(ctx);
		}
		// find a location in memory where we fit
		int offset = 0;
		for (int i = 0; i < memory.size(); i++) {
			offset = memory.get(i).start + memory.get(i).size;
			if (i + 1 < memory.size() && ((offset + size) < memory.get(i + 1).start)) {
				// add to mem
				memory.add(new Block(offset, size, ctx));
				// return offset
				Collections.sort(memory);
				return offset;
			}
		}
		// if not returned we need to add at the tail.
		// offset calculated in the last for loop
		memory.add(new Block(offset, size, ctx));
		return offset;
	}

	/**
	 * Get the offset of an variable or reserve new memory.
	 */
	public int getOffset(ParseTree ctx, int size) {
		for (Block b : memory) {
			if (b.ctx.equals(ctx)) {
				return b.start;
			}
		}
		return reserveMemory(ctx, size);
	}

	/**
	 * Check whether this variable as memory associated with it
	 */
	public boolean hasMemory(ParseTree ctx) {
		for (Block b : memory) {
			if (b.ctx.equals(ctx)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * frees the memory segment reserved by the given id for further use.
	 */
	private void freeMemory(ParseTree ctx) {
		for (Block b : memory) {
			if (b.ctx.equals(ctx)) {
				memory.remove(b);
				return;
			}
		}
	}

	/**
	 * Opens a new scope.
	 */
	public void openScope() {
		varScopes.push(new ArrayList<ParseTree>());
		conScopes.push(new ArrayList<String>());
	}

	/**
	 * Closes the scope and frees all memory blocks related to the variables on
	 * this scope. Also clears all relevant registers for further use.
	 */
	public void closeScope() {
		ArrayList<String> conold = conScopes.pop();
		for (String s : conold) {
			regman.freeReg(null, s, true);
		}
		ArrayList<ParseTree> varold = varScopes.pop();
		for (ParseTree ctx : varold) {
			freeMemory(ctx);
			regman.freeReg(ctx, null, false);
		}

	}

	/**
	 * Forward to the registermanager.
	 */
	public String getVarReg(ParseTree ctx) {
		// add to scope if necesary
		if (!inScope(ctx)) {
			varScopes.getFirst().add(ctx);
		}
		return regman.getOrReserveReg(ctx);
	}

	/**
	 * Reserve a register for a constant value, e.g. '5'. Will be freed when the
	 * current scope is closed
	 */
	public String getConstReg() {
		String reg = regman.getConstReg();
		conScopes.getFirst().add(reg);
		return reg;
	}
}
