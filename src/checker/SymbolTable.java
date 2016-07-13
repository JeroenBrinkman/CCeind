package checker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple layered symbol table. Keeps track of id's and types.
 * 
 * @author Jeroen
 *
 */
public class SymbolTable {

	private final Deque<Map<String, Type>> stack = new ArrayDeque<Map<String, Type>>();

	/**
	 * Opens a scope
	 */
	public void openScope() {
		stack.push(new HashMap<String, Type>());
	}

	/**
	 * Closes a scope, removing all symbols that were a part of this scope from
	 * the table
	 */
	public void closeScope() {
		stack.pop();
	}

	/**
	 * Attemps to add a new symbol to the table, only succeeds if the id had not
	 * yet been added
	 * 
	 * @requires id != null
	 * @requires type != null
	 * @return true if !contains(id)
	 */
	public boolean add(String id, Type type) {
		if (contains(id)) {
			return false;

		} else {
			Map<String, Type> currentScope;
			currentScope = stack.getFirst();
			currentScope.put(id, type);
			return true;
		}
	}

	/**
	 * Checks whether an id is one of the current scopes
	 * 
	 * @requires id != null
	 */
	public boolean contains(String id) {
		Map<String, Type> currentScope;
		for (Iterator<Map<String, Type>> itr = stack.iterator(); itr.hasNext();) {
			currentScope = (Map<String, Type>) itr.next();
			if (currentScope.containsKey(id)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the type of the given id, returns void if it is not in scope
	 * 
	 * @requires id != null
	 * @ensures result != null
	 */
	public Type getType(String id) {
		Type type = Type.VOID;
		Map<String, Type> currentScope;
		for (Iterator<Map<String, Type>> itr = stack.descendingIterator(); itr.hasNext();) {
			currentScope = (Map<String, Type>) itr.next();
			if (currentScope.containsKey(id)) {
				type = currentScope.get(id);
			}
		}
		return type;
	}

}
