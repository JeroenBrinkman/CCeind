package checker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SymbolTable{

	private final Deque<Map<String, Type>> stack = new ArrayDeque<Map<String,Type>>();
	private List<String> constants = new ArrayList<String>();
	
	public void openScope() {
		stack.push(new HashMap<String, Type>());
	}

	public void closeScope() {
		stack.pop();
	}

	public boolean add(String id, Type type) {
		if(contains(id) || isConst(id)){
			return false;
			
		}else{
			Map<String, Type> currentScope;
			currentScope = stack.getFirst();
			currentScope.put(id, type);
			return true;
		}
	}
	
	public void addConst(String id){
		constants.add(id);
	}

	public boolean contains(String id) {
		Map<String, Type> currentScope;
		for(Iterator<Map<String, Type>> itr = stack.iterator(); itr.hasNext();){
			currentScope = (Map<String, Type>) itr.next();
			if(currentScope.containsKey(id)){
				return true;
			}
		}
		return false;
	}
	
	public boolean isConst(String id){
		return constants.contains(id);
	}
	
	public Type getType(String id){
		Type type = Type.VOID;
		Map<String, Type> currentScope;
		for(Iterator<Map<String, Type>> itr = stack.descendingIterator(); itr.hasNext();){
			currentScope = (Map<String, Type>) itr.next();
			if(currentScope.containsKey(id)){
				type = currentScope.get(id);
			}
		}
		return type;
	}
	
}
