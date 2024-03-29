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
package ghidra.program.database.function;

import static ghidra.program.util.FunctionChangeRecord.FunctionChangeType.*;

import java.io.IOException;

import ghidra.program.model.data.*;
import ghidra.program.model.lang.DynamicVariableStorage;
import ghidra.program.model.listing.*;
import ghidra.program.model.symbol.SourceType;
import ghidra.util.exception.DuplicateNameException;
import ghidra.util.exception.InvalidInputException;

public class ReturnParameterDB extends ParameterDB {

	private DataType dataType;

	ReturnParameterDB(FunctionDB function, DataType dt, VariableStorage storage) {
		super(function, null);
		this.dataType = dt;
		this.storage = storage;
	}

	@Override
	protected boolean isVoidAllowed() {
		return true;
	}

	@Override
	public String getName() {
		return RETURN_NAME;
	}

	@Override
	public void setName(String name, SourceType source)
			throws DuplicateNameException, InvalidInputException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getComment() {
		return null;
	}

	@Override
	public void setComment(String comment) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int getOrdinal() {
		return RETURN_ORIDINAL;
	}

	@Override
	final void setOrdinal(int ordinal) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDataType(DataType type, VariableStorage newStorage, boolean force,
			SourceType source) throws InvalidInputException, VariableSizeException {
		functionMgr.lock.acquire();
		try {
			function.checkDeleted();
			boolean hasCustomStorage = function.hasCustomVariableStorage();
			if (!hasCustomStorage) {
				newStorage = VariableStorage.UNASSIGNED_STORAGE;
			}
			Program program = function.getProgram();
			type = VariableUtilities.checkDataType(type,
				newStorage.isVoidStorage() || newStorage.isUnassignedStorage(), getLength(),
				program);
			if (!newStorage.isUnassignedStorage()) {
				newStorage = VariableUtilities.checkStorage(function, newStorage, type, force);
			}
			function.setReturnStorageAndDataType(newStorage, type);
			this.dataType = program.getDataTypeManager().resolve(type, null);
			if (hasCustomStorage) {
				this.storage = newStorage;
			}
			else {
				this.storage = VariableStorage.UNASSIGNED_STORAGE;
				function.updateParametersAndReturn();
			}
			function.updateSignatureSourceAfterVariableChange(source, type);
			functionMgr.functionChanged(function, RETURN_TYPE_CHANGED);
		}
		catch (IOException e) {
			functionMgr.dbError(e);
		}
		finally {
			functionMgr.lock.release();
		}
	}

	@Override
	public void setDataType(DataType type, SourceType source) throws InvalidInputException {
		setDataType(type, true, false, source);
	}

	@Override
	public void setDataType(DataType type, boolean alignStack, boolean force, SourceType source)
			throws InvalidInputException {
		functionMgr.lock.acquire();
		try {
			function.checkDeleted();
			Program program = function.getProgram();
			type = VariableUtilities.checkDataType(type, true, 0, program);
			VariableStorage newStorage = VariableStorage.UNASSIGNED_STORAGE;
			boolean hasCustomVariableStorage = function.hasCustomVariableStorage();
			if (hasCustomVariableStorage) {
				try {
					newStorage = VoidDataType.isVoidDataType(type) ? VariableStorage.VOID_STORAGE
							: VariableUtilities.resizeStorage(getVariableStorage(), type,
								alignStack, function);
					VariableUtilities.checkStorage(newStorage, type, force);
				}
				catch (InvalidInputException e) {
					// If forced - use Unassigned storage
					if (!force) {
						throw e;
					}
					newStorage = VariableStorage.UNASSIGNED_STORAGE;
				}
				this.storage = newStorage;
			}
			else {
				this.storage = VariableStorage.UNASSIGNED_STORAGE;
			}
			function.setReturnStorageAndDataType(newStorage, type);
			this.dataType = program.getDataTypeManager().resolve(type, null);
			if (!hasCustomVariableStorage) {
				function.updateParametersAndReturn();
			}
			function.updateSignatureSourceAfterVariableChange(source, type);
			functionMgr.functionChanged(function, RETURN_TYPE_CHANGED);
		}
		catch (IOException e) {
			functionMgr.dbError(e);
		}
		finally {
			functionMgr.lock.release();
		}
	}

	@Override
	public DataType getFormalDataType() {
		return dataType;
	}

	@Override
	public DataType getDataType() {
		if (storage == DynamicVariableStorage.INDIRECT_VOID_STORAGE) {
			return VoidDataType.dataType;
		}
		return super.getDataType();
	}

	@Override
	public boolean isForcedIndirect() {
		return storage.isForcedIndirect();
	}

	@Override
	public SourceType getSource() {
		if (dataType == null || Undefined.isUndefined(dataType)) {
			return SourceType.DEFAULT;
		}
		return function.getSignatureSource();
	}

	@Override
	public boolean hasAssignedStorage() {
		return function.hasCustomVariableStorage() && !storage.isUnassignedStorage();
	}

	@Override
	public VariableStorage getVariableStorage() {
		return storage;
	}

	@Override
	void setStorageAndDataType(VariableStorage newStorage, DataType dt) {
		if (!function.hasCustomVariableStorage()) {
			newStorage = VariableStorage.UNASSIGNED_STORAGE;
		}
		try {
			function.setReturnStorageAndDataType(newStorage, dt);
			storage = newStorage;
			dataType = dt;
		}
		catch (IOException e) {
			function.getFunctionManager().dbError(e);
		}
	}

}
