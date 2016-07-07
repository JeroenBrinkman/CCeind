package generator;

import java.util.ArrayList;

import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Manages register usage. Limits the maximum amount of registers to MAXREG.
 * getOrReserveRegister is the recommended method for reserving variables.
 * Constants are //TODO
 *
 * @author Jeroen
 */
public class RegisterManager {
	/**
	 * Abstraction of the registers
	 *
	 */
	private class Register implements Comparable<Register> {
		public int number;
		public ParseTree ctx;
		public boolean constant = false;

		public Register(int number, ParseTree ctx) {
			this.number = number;
			this.ctx = ctx;
		}

		@Override
		public String toString() {
			return "r_" + number;
		}

		@Override
		public int compareTo(Register arg0) {
			return this.number - arg0.number;
		}
	}

	private final ArrayList<Register> registers = new ArrayList<Register>();
	private static final int MAXREG = 30;

	public RegisterManager() {
		for (int i = 0; i < MAXREG; i++) {
			registers.add(new Register(i, null));
		}
	}

	/**
	 * Get an existing register for a variable that already has a register.
	 * returns null if the variable has no register
	 */
	public String getReg(ParseTree ctx) {
		for (Register r : registers) {

			if (r.ctx != null && r.ctx.equals(ctx)) {
				return r.toString();
			}
		}
		return null;
	}

	/**
	 * Gets the existing register for a variable or reserves a new register if
	 * this does not exist.
	 */
	public String getOrReserveReg(ParseTree ctx) {
		String reg = getReg(ctx);
		if (reg == null) {
			reg = reserveReg(ctx);
		}
		return reg;
	}

	/**
	 * Reserve a new register for a variable.
	 */
	private String reserveReg(ParseTree ctx) {
		Register r = null;
		for (int i = 0; i < MAXREG; i++) {
			r = registers.get(i);
			if (r.ctx == null && !r.constant) {
				r.ctx = ctx;
				break;
			}
		}
		// TODO throw out of registers exception?
		// Should we even limit registers?
		return r.toString();
	}

	/**
	 * Free a register for a variable.
	 */
	public void freeReg(ParseTree ctx, String name, boolean constant) {
		if (constant) {
			for (Register r : registers) {
				if (r.ctx.equals(ctx) && r.constant == constant) {
					r.ctx = null;
					return;
				}
			}
		} else {
			for (Register r : registers) {
				if (r.toString().equals(name) && r.constant == constant) {
					r.ctx = null;
					r.constant = false;
				}
			}
		}
	}

	public String getConstReg() {
		Register r = null;
		for (int i = 0; i < MAXREG; i++) {
			r = registers.get(i);
			if (r.ctx == null && !r.constant) {
				r.constant = true;
				return r.toString();
			}
		}
		// TODO throw out of registers exception?
		// Should we even limit registers?
		return r.toString();
	}

	public String toString() {
		String out = "Registers : \n";
		for (Register r : registers) {
			out = out + r.toString() + " " + (r.constant ? "constant" : (r.ctx == null ? "null" : r.ctx.getText()) + "\n");
		}
		return out;
	}
}
