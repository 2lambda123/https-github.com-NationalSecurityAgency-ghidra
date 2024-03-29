/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.app.plugin.assembler.sleigh.expr;

import java.util.*;

import ghidra.app.plugin.assembler.sleigh.sem.*;
import ghidra.app.plugin.processors.sleigh.Constructor;
import ghidra.app.plugin.processors.sleigh.expression.OperandValue;
import ghidra.app.plugin.processors.sleigh.expression.PatternExpression;
import ghidra.app.plugin.processors.sleigh.symbol.OperandSymbol;
import ghidra.app.plugin.processors.sleigh.symbol.TripleSymbol;

//Based on OperandValue#getValue()
/**
 * Solves expressions of an operand value
 * 
 * <p>
 * These are a sort of named sub-expression, but they may also specify a shift in encoding.
 */
public class OperandValueSolver extends AbstractExpressionSolver<OperandValue> {

	public OperandValueSolver() {
		super(OperandValue.class);
	}

	/**
	 * Obtains the "defining expression"
	 * 
	 * <p>
	 * This is either the symbol's assigned defining expression, or the expression associated with
	 * its defining symbol.
	 * 
	 * @return the defining expression, or null if neither is available
	 */
	public static PatternExpression getDefiningExpression(OperandSymbol sym) {
		PatternExpression patexp = sym.getDefiningExpression();
		if (patexp != null) {
			return patexp;
		}
		TripleSymbol defSym = sym.getDefiningSymbol();
		if (defSym == null) {
			return null;
		}
		patexp = defSym.getPatternExpression();
		return patexp;
	}

	@Override
	public AssemblyResolution solve(AbstractAssemblyResolutionFactory<?, ?> factory,
			OperandValue ov, MaskedLong goal, Map<String, Long> vals, AssemblyResolvedPatterns cur,
			Set<SolverHint> hints, String description) throws NeedsBackfillException {
		Constructor cons = ov.getConstructor();
		OperandSymbol sym = cons.getOperand(ov.getIndex());
		PatternExpression patexp = getDefiningExpression(sym);
		if (patexp == null) {
			if (goal.equals(MaskedLong.ZERO)) {
				return factory.nop(description);
			}
			return factory.newErrorBuilder()
					.error("Operand " + sym.getName() +
						" is undefined and does not agree with child requirements")
					.description(description)
					.build();
		}
		AssemblyResolution result =
			solver.solve(factory, patexp, goal, vals, cur, hints, description);
		if (result.isError()) {
			AssemblyResolvedError err = (AssemblyResolvedError) result;
			return factory.newErrorBuilder()
					.error(err.getError())
					.description("Solution to " + sym.getName() + " := " + goal + " = " + patexp)
					.children(List.of(result))
					.build();
		}
		// TODO: Shifting here seems like a hack to me.
		// I assume this only comes at the top of an expression
		AssemblyResolvedPatterns con = (AssemblyResolvedPatterns) result;
		int shamt = AssemblyTreeResolver.computeOffset(sym, cons);
		return con.shift(shamt);
	}

	@Override
	public MaskedLong getValue(OperandValue ov, Map<String, Long> vals,
			AssemblyResolvedPatterns cur) throws NeedsBackfillException {
		Constructor cons = ov.getConstructor();
		OperandSymbol sym = cons.getOperand(ov.getIndex());
		PatternExpression patexp = getDefiningExpression(sym);
		if (patexp == null) {
			return MaskedLong.ZERO;
		}
		int shamt = AssemblyTreeResolver.computeOffset(sym, cons);
		cur = cur == null ? null : cur.truncate(shamt);
		MaskedLong result = solver.getValue(patexp, vals, cur);
		return result;
	}

	@Override
	public int getInstructionLength(OperandValue ov) {
		Constructor cons = ov.getConstructor();
		OperandSymbol sym = cons.getOperand(ov.getIndex());
		PatternExpression patexp = sym.getDefiningExpression();
		if (patexp == null) {
			return 0;
		}
		int length = solver.getInstructionLength(patexp);
		int shamt = AssemblyTreeResolver.computeOffset(sym, cons);
		return length + shamt;
	}

	@Override
	public MaskedLong valueForResolution(OperandValue ov, Map<String, Long> vals,
			AssemblyResolvedPatterns rc) {
		Constructor cons = ov.getConstructor();
		OperandSymbol sym = cons.getOperand(ov.getIndex());
		PatternExpression patexp = sym.getDefiningExpression();
		if (patexp != null) {
			// We're good to go
		}
		else {
			TripleSymbol defSym = sym.getDefiningSymbol();
			if (defSym != null) {
				patexp = defSym.getPatternExpression();
			}
		}
		if (patexp == null) {
			return MaskedLong.ZERO; // TODO: ZERO or UNKS?
		}
		// TODO: Can just shift the rc to the left the appropriate number of bytes.
		// Would only affect the instruction block.
		// Since I'm using this just for context, ignore shifting for now.
		//int shamt = AssemblyTreeResolver.computeOffset(sym, cons, rc.children);
		// Children would be null here, anyway.
		return solver.valueForResolution(patexp, vals, rc);
		// NOTE: To be paranoid, I could check for the existence of TokenField in the expression
		// And also check if a shift would be performed.
	}
}
