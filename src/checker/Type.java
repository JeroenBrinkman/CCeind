package checker;

import iloc.eval.Machine;

/** TempName data type. */
abstract public class Type {
	/** The singleton instance of the {@link Bool} type. */
	public static final Type BOOL = new Bool();
	/** The singleton instance of the {@link Int} type. */
	public static final Type INT = new Int();
	/** The singleton instance of the {@link Char} type. */
	public static final Type CHAR = new Char();
	/** The singleton instance of the {@link String} type. */
	public static final Type STRING = new String();
	/** The singleton instance of the {@link Void} type. */
	public static final Type VOID = new Void();
	
	private final TypeKind kind;

	/** Constructor for subclasses. */
	protected Type(TypeKind kind) {
		this.kind = kind;
	}

	/** Returns the kind of this type. */
	public TypeKind getKind() {
		return this.kind;
	}

	/** returns the size (in bytes) of a value of this type. */
	abstract public int size();

	/** Representation of the Boolean type. */
	static public class Bool extends Type {
		private Bool() {
			super(TypeKind.BOOL);
		}

		@Override
		public int size() {
			return Machine.INT_SIZE;
		}

		@Override
		public java.lang.String toString() {
			return "Boolean";
		}
	}

	/** Representation of the Integer type. */
	static public class Int extends Type {
		private Int() {
			super(TypeKind.INT);
		}

		@Override
		public int size() {
			return Machine.INT_SIZE;
		}

		@Override
		public java.lang.String toString() {
			return "Integer";
		}
	}
	
	/** Representation of the Char type. */
	static public class Char extends Type{

		private Char(){
			super(TypeKind.CHAR);
		}
		
		@Override
		public int size() {
			return Machine.DEFAULT_CHAR_SIZE;
		}

		@Override
		public java.lang.String toString() {
			return "Character";
		}
		
	}
	
	/** Representation of the String type. */
	static public class String extends Type{
		private int length = 0;
		private String(){
			super(TypeKind.STRING);
		}
		
		@Override
		public int size() {
			return length*Machine.DEFAULT_CHAR_SIZE;
		}

		@Override
		public java.lang.String toString() {
			return "String";
		}
		
		public void setLength(int length){
			this.length = length;
			
		}
		
	}
	
	/** Representation of the Void type. */
	static public class Void extends Type{

		private Void(){
			super(TypeKind.VOID);
		}
		
		@Override
		public int size() {
			return 0;
		}

		@Override
		public java.lang.String toString() {
			return "Void";
		}
		
	}
}
