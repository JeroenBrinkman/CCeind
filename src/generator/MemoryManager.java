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
		public String id;

		public Block(int st, int si, ParseTree ctx, String id) {
			start = st;
			size = si;
			this.ctx = ctx;
			this.id = id;
			;
		}

		@Override
		public int compareTo(Block arg0) {
			return this.start - ((Block) arg0).start;
		}

		@Override
		public String toString() {
			String out = "";
			out = out + "\t--------------\n";
			out = out + "\tstart = \t" + start + "\n";
			out = out + "\tsize = \t" + size + "\n";
			out = out + "\tid = \t" + id + "\n";
			out = out + "\tctx = \t" + ctx.getText() + "\n";
			out = out + "\t--------------";
			return out;
		}
	}

	private final Deque<ArrayList<ParseTree>> nodeScopes = new ArrayDeque<ArrayList<ParseTree>>();
	private final Deque<ArrayList<String>> conScopes = new ArrayDeque<ArrayList<String>>();
	private final Deque<ArrayList<String>> idScopes = new ArrayDeque<ArrayList<String>>();
	private final ArrayList<Block> memory;
	private final RegisterManager regman;

	public MemoryManager() {
		memory = new ArrayList<Block>();
		regman = new RegisterManager();
	}

	private boolean inScope(ParseTree ctx) {
		ArrayList<ParseTree> scope;
		for (Iterator<ArrayList<ParseTree>> itr = nodeScopes.iterator(); itr.hasNext();) {
			scope = itr.next();
			if (scope.contains(ctx)) {
				return true;
			}
		}
		return false;
	}

	private boolean inScope(String id) {
		ArrayList<String> scope;
		for (Iterator<ArrayList<String>> itr = idScopes.iterator(); itr.hasNext();) {
			scope = itr.next();
			if (scope.contains(id)) {
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
			nodeScopes.getFirst().add(ctx);
		}
		// find a location in memory where we fit
		int offset = 0;
		for (int i = 0; i < memory.size(); i++) {
			offset = memory.get(i).start + memory.get(i).size;
			if (i + 1 < memory.size() && ((offset + size) < memory.get(i + 1).start)) {
				// add to mem
				memory.add(new Block(offset, size, ctx, null));
				// return offset
				Collections.sort(memory);
				return offset;
			}
		}
		// if not returned we need to add at the tail.
		// offset calculated in the last for loop
		memory.add(new Block(offset, size, ctx, null));
		return offset;
	}

	/**
	 * reserves a memory block for the given id at the given size. When the size
	 * changes it should perform a free and new reserve action. Returns the
	 * offset at which this id can write to memory
	 * 
	 * @requires id != null
	 */
	public int reserveMemory(String id, int size, ParseTree ctx) {
		// add to scope if not already contained
		if (!inScope(id)) {
			idScopes.getFirst().add(id);
		}
		// find a location in memory where we fit
		int offset = 0;
		for (int i = 0; i < memory.size(); i++) {
			offset = memory.get(i).start + memory.get(i).size;
			if (i + 1 < memory.size() && ((offset + size) < memory.get(i + 1).start)) {
				// add to mem
				memory.add(new Block(offset, size, ctx, id));
				// return offset
				Collections.sort(memory);
				return offset;
			}
		}
		// if not returned we need to add at the tail.
		// offset calculated in the last for loop
		memory.add(new Block(offset, size, ctx, id));
		return offset;
	}

	/**
	 * Get the offset of an variable or reserve new memory.
	 * 
	 * @requires ctx != null || id != null
	 */
	public int getOffset(ParseTree ctx, int size, String id) {
		if (id == null) {
			for (Block b : memory) {
				if (b.ctx != null && b.ctx.equals(ctx)) {
					if (b.size == size) {
						return b.start;
					} else {
						this.freeMemory(ctx);
						return reserveMemory(ctx, size);
					}
				}
			}
			return reserveMemory(ctx, size);
		} else {
			for (Block b : memory) {
				if (b.id != null && b.id.equals(id)) {
					if (b.size == size) {
						return b.start;
					} else {
						freeMemory(id);
						reserveMemory(id, size, ctx);
					}
				}
			}
			return reserveMemory(id, size, ctx);
		}
	}

	/**
	 * Returns the offset and size of an object in memory
	 * 
	 * @requires has Memory(ctx) || has Memory(id)
	 */
	public int[] getSizeAndOffset(ParseTree ctx, String id) {
		int[] out = new int[2];
		if (id == null) {
			for (Block b : memory) {
				if (b.ctx != null && b.ctx.equals(ctx)) {
					out[0] = b.size;
					out[1] = b.start;
					return out;
				}
			}
		} else {
			for (Block b : memory) {
				if (b.id != null && b.id.equals(id)) {
					out[0] = b.size;
					out[1] = b.start;
					return out;
				}
			}
		}
		return null;
	}

	/**
	 * Check whether this variable as memory associated with it
	 */
	public boolean hasMemory(ParseTree ctx) {
		for (Block b : memory) {
			if (b.ctx != null && b.ctx.equals(ctx)) {
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
			if (b.ctx != null && b.ctx.equals(ctx)) {
				memory.remove(b);
				return;
			}
		}
	}

	/**
	 * frees the memory segment reserved by the given id for further use.
	 */
	private void freeMemory(String id) {
		for (Block b : memory) {
			if (b.id != null && b.id.equals(id)) {
				memory.remove(b);
				return;
			}
		}
	}

	/**
	 * Opens a new scope.
	 */
	public void openScope() {
		nodeScopes.push(new ArrayList<ParseTree>());
		conScopes.push(new ArrayList<String>());
		idScopes.push(new ArrayList<String>());
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
		ArrayList<ParseTree> varold = nodeScopes.pop();
		for (ParseTree ctx : varold) {
			freeMemory(ctx);
			regman.freeReg(ctx, null, false);
		}
		ArrayList<String> idold = idScopes.pop();
		for (String s : idold) {
			freeMemory(s);
		}

	}

	/**
	 * Forward to the registermanager.
	 */
	public String getNodeReg(ParseTree ctx) {
		// add to scope if necesary
		if (!inScope(ctx)) {
			nodeScopes.getFirst().add(ctx);
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

	public boolean hasReg(ParseTree ctx) {
		return regman.getReg(ctx) != null;
	}

	@Override
	public String toString() {
		String out = "Memory model:\n";
		for (Block b : memory) {
			out = out + b.toString();
		}
		out = out + "\n" + regman.toString();
		return out;
	}

	public String toString(boolean mem, boolean reg) {
		String out = "";
		if (mem) {
			out = "Memory model:\n";
			for (Block b : memory) {
				out = out + b.toString();
			}
		}
		if (reg) {
			out = out + "\n" + regman.toString();
		}
		return out;
	}
}
