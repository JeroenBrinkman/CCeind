package generator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;

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
		public String id;

		public Block(int st, int si, String id) {
			start = st;
			size = si;
			this.id = id;
			;
		}

		@Override
		public int compareTo(Block arg0) {
			return this.start - ((Block) arg0).start;
		}
	}

	private final Deque<ArrayList<String>> scopes = new ArrayDeque<ArrayList<String>>();
	private final ArrayList<Block> memory;
	private final RegisterManager regman;

	public MemoryManager() {
		scopes.push(new ArrayList<String>());
		memory = new ArrayList<Block>();
		regman = new RegisterManager();
	}

	/**
	 * reserves a memory block for the given id at the given size. When the size
	 * changes it should perform a free and new reserve action. Returns the
	 * offset at which this id can write to memory
	 */
	public int reserveMemory(String id, int size) {
		// add to scope
		scopes.getFirst().add(id);
		// find a location in memory where we fit
		int offset = 0;
		for (int i = 0; i < memory.size(); i++) {
			offset = memory.get(i).start + memory.get(i).size;
			if (i + 1 < memory.size() && ((offset + size) < memory.get(i + 1).start)) {
				// add to mem
				memory.add(new Block(offset, size, id));
				// return offset
				Collections.sort(memory);
				return offset;
			}
		}
		// if not returned we need to add at the tail.
		// offset calculated in the last for loop
		memory.add(new Block(offset, size, id));
		return offset;
	}

	/**
	 * Get the offset of an already reserved variable
	 */
	public int getOffset(String id) {
		for (Block b : memory) {
			if (b.id.equals(id)) {
				return b.start;
			}
		}
		return -1;
	}

	/**
	 * Check whether this variable as memory associated with it
	 */
	public boolean hasMemory(String id) {
		for (Block b : memory) {
			if (b.id.equals(id)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * frees the memory segment reserved by the given id for further use.
	 */
	public void freeMemory(String id) {
		for (Block b : memory) {
			if (b.id.equals(id)) {
				memory.remove(b);
				return;
			}
		}
	}

	/**
	 * Opens a new scope.
	 */
	public void openScope() {
		scopes.push(new ArrayList<String>());
	}

	/**
	 * Closes the scope and frees all memory blocks related to the variables on
	 * this scope. Also clears all relevant registers for further use.
	 */
	public void closeScope() {
		ArrayList<String> old = scopes.pop();
		for (String s : old) {
			freeMemory(s);
			regman.freeReg(s, false);
		}
	}

	/**
	 * Forward to the registermanager.
	 */
	public String getReg(String id) {
		return regman.getOrReserveReg(id);
	}

	/**
	 * Forward to the register manager. Frees the register belonging to a
	 * variable.
	 */
	public void freeVarReg(String id) {
		regman.freeReg(id, false);
	}
}
