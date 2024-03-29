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
package ghidra.app.plugin.assembler.sleigh.tree;

import java.io.PrintStream;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import ghidra.app.plugin.assembler.sleigh.grammars.*;
import ghidra.app.plugin.assembler.sleigh.sem.AssemblyConstructorSemantic;
import ghidra.app.plugin.assembler.sleigh.symbol.AssemblyNonTerminal;
import ghidra.app.plugin.assembler.sleigh.symbol.AssemblySymbol;

/**
 * A branch in a parse tree, corresponding to the application of a production
 */
public class AssemblyParseBranch extends AssemblyParseTreeNode
		implements Iterable<AssemblyParseTreeNode> {
	// The substitutions, each corresponding to a symbol from the production's RHS
	private final List<AssemblyParseTreeNode> substs = new ArrayList<>();
	// The production applied to create this branch
	private final AssemblyProduction prod;

	/**
	 * Construct a branch from the given grammar and production
	 * 
	 * @param grammar the grammar containing the production
	 * @param prod the production applied to create this branch
	 */
	public AssemblyParseBranch(AssemblyGrammar grammar, AssemblyProduction prod) {
		super(grammar);
		this.prod = prod;
	}

	@Override
	public int hashCode() {
		int result = prod.hashCode();
		for (AssemblyParseTreeNode n : substs) {
			result *= 31;
			result += n.hashCode();
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		AssemblyParseBranch that = (AssemblyParseBranch) obj;
		if (!this.substs.equals(that.substs)) {
			return false;
		}
		return true;
	}

	/**
	 * Prepend a child to this branch
	 * 
	 * <p>
	 * Because LR parsers produce rightmost derivations, they necessarily populate the branches
	 * right to left. During reduction, each child is popped from the stack, traversing them in
	 * reverse order. This method prepends children so that when reduction is complete, the children
	 * are aligned to the corresponding symbols from the RHS of the production.
	 * 
	 * @param child the child
	 */
	public void addChild(AssemblyParseTreeNode child) {
		assert expects().equals(child.getSym());
		this.substs.add(0, child);
		child.setParent(this);
	}

	/**
	 * See what symbol is expected next
	 * 
	 * <p>
	 * The child added next must be associated with the token expected next.
	 * 
	 * @return the symbol
	 */
	protected AssemblySymbol expects() {
		if (!isComplete()) {
			AssemblySentential<?> rhs = prod.getRHS();
			return rhs.getSymbol(rhs.size() - substs.size() - 1);
		}
		return null;
	}

	/**
	 * Check if the branch is full
	 * 
	 * @return true if every symbol on the RHS has a corresponding child
	 */
	protected boolean isComplete() {
		return prod.getRHS().size() == substs.size();
	}

	@Override
	public AssemblyNonTerminal getSym() {
		return prod.getLHS();
	}

	@Override
	protected void print(PrintStream out, String indent) {
		out.print(indent + getSym() + " := " + prod);
		Collection<AssemblyConstructorSemantic> sems = grammar.getSemantics(prod);
		if (!sems.isEmpty()) {
			out.print(" (" + StringUtils.join(sems, ", ") + ")");
		}
		out.println();
		for (AssemblyParseTreeNode s : substs) {
			s.print(out, "  " + indent);
		}
	}

	@Override
	public String toString() {
		return this.prod.getLHS().toString();
	}

	/**
	 * Get the production applied to create this branch
	 * 
	 * @return
	 */
	public AssemblyProduction getProduction() {
		return prod;
	}

	/**
	 * Get the list of children, indexed by corresponding symbol from the RHS
	 * 
	 * @return
	 */
	public List<AssemblyParseTreeNode> getSubstitutions() {
		return Collections.unmodifiableList(substs);
	}

	@Override
	public Iterator<AssemblyParseTreeNode> iterator() {
		return getSubstitutions().iterator();
	}

	/**
	 * Get the <em>i</em>th child, corresponding to the <em>i</em>th symbol from the RHS
	 * 
	 * @param i the position
	 * @return the child
	 */
	public AssemblyParseTreeNode getSubstitution(int i) {
		return substs.get(i);
	}

	public boolean isConstructor() {
		return prod.isConstructor();
	}

	@Override
	public String generateString() {
		StringBuilder sb = new StringBuilder();
		for (AssemblyParseTreeNode node : substs) {
			sb.append(node.generateString());
		}
		return sb.toString();
	}
}
