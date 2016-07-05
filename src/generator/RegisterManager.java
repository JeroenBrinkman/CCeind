package generator;

import java.util.ArrayList;

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
		public String id;
		public boolean constant = false;

		public Register(int number, String id) {
			this.number = number;
			this.id = id;
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
	private static final int MAXREG = 75;

	public RegisterManager() {
		for (int i = 0; i < MAXREG; i++) {
			registers.add(new Register(i, null));
		}
	}

	/**
	 * Get an existing register for a variable that already has a register.
	 * returns null if the variable has no register
	 */
	public String getReg(String id) {
		for (Register r : registers) {
			if (r.id.equals(id)) {
				return r.toString();
			}
		}
		return null;
	}

	/**
	 * Gets the existing register for a variable or reserves a new register if
	 * this does not exist.
	 */
	public String getOrReserveReg(String id) {
		String reg = getReg(id);
		if (reg == null) {
			reg = reserveReg(id);
		}
		return reg;
	}

	/**
	 * Reserve a new register for a variable.
	 */
	public String reserveReg(String id) {
		Register r = null;
		for (int i = 0; i < MAXREG; i++) {
			r = registers.get(i);
			if (r.id == null && !r.constant) {
				r.id = id;
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
	public void freeReg(String id, boolean constant) {
		if (constant) {
			for (Register r : registers) {
				if (r.id.equals(id) && r.constant == constant) {
					r.id = null;
					return;
				}
			}
		} else {
			for (Register r : registers) {
				if (r.toString().equals(id) && r.constant == constant) {
					r.id = null;
					r.constant = false;
				}
			}
		}
	}

	public String getConstReg() {
		Register r = null;
		for (int i = 0; i < MAXREG; i++) {
			r = registers.get(i);
			if (r.id == null && !r.constant) {
				r.id = r.toString();
				r.constant = true;
				return r.toString();
			}
		}
		// TODO throw out of registers exception?
		// Should we even limit registers?
		return r.toString();
	}
}
