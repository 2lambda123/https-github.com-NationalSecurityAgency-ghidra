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

import java.util.Map;
import java.util.Set;

import ghidra.app.plugin.assembler.sleigh.sem.*;
import ghidra.app.plugin.processors.sleigh.expression.Next2InstructionValue;

/**
 * "Solves" expressions of {@code inst_next2}
 * 
 * <p>
 * Works like the constant solver, but takes the value of {@code inst_next}, which is given by the
 * assembly address and the resulting instruction length.
 * 
 * <p>
 * <b>NOTE:</b> This solver requires backfill, since the value of {@code inst_next2} is not known
 * until possible prefixes have been considered.
 */
public class Next2InstructionValueSolver extends AbstractExpressionSolver<Next2InstructionValue> {

	public Next2InstructionValueSolver() {
		super(Next2InstructionValue.class);
	}

	@Override
	public AssemblyResolution solve(AbstractAssemblyResolutionFactory<?, ?> factory,
			Next2InstructionValue exp, MaskedLong goal, Map<String, Long> vals,
			AssemblyResolvedPatterns cur, Set<SolverHint> hints, String description)
			throws NeedsBackfillException {
		throw new AssertionError(
			"INTERNAL: Should never be asked to solve for " + AssemblyTreeResolver.INST_NEXT2);
	}

	@Override
	public MaskedLong getValue(Next2InstructionValue iv, Map<String, Long> vals,
			AssemblyResolvedPatterns cur) throws NeedsBackfillException {
		Long instNext = vals.get(AssemblyTreeResolver.INST_NEXT2);
		if (instNext == null) {
			throw new NeedsBackfillException(AssemblyTreeResolver.INST_NEXT2);
		}
		return MaskedLong.fromLong(instNext);
	}

	@Override
	public int getInstructionLength(Next2InstructionValue iv) {
		return 0;
	}

	@Override
	public MaskedLong valueForResolution(Next2InstructionValue exp, Map<String, Long> vals,
			AssemblyResolvedPatterns rc) {
		Long instNext = vals.get(AssemblyTreeResolver.INST_NEXT2);
		if (instNext == null) {
			/**
			 * This method is used in forward state construction, so just leave unknown. This may
			 * cause unresolvable trees to get generated, but we can't know that until we try to
			 * resolve them.
			 */
			return MaskedLong.UNKS;
		}
		return MaskedLong.fromLong(instNext);
	}
}
