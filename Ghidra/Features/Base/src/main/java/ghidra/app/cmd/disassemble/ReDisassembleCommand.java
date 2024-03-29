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
package ghidra.app.cmd.disassemble;

import ghidra.framework.cmd.BackgroundCommand;
import ghidra.framework.model.DomainObject;
import ghidra.program.disassemble.ReDisassembler;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;

public class ReDisassembleCommand extends BackgroundCommand {
	private final Address seed;

	public ReDisassembleCommand(Address seed) {
		this.seed = seed;
	}

	@Override
	public boolean applyTo(DomainObject obj, TaskMonitor monitor) {
		ReDisassembler dis = new ReDisassembler((Program) obj);
		try {
			dis.disasemble(seed, monitor);
			return true;
		}
		catch (CancelledException e) {
			return false;
		}
	}
}
