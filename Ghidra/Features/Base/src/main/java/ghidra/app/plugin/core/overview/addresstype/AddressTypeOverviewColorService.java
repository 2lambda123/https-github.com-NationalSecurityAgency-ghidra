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
package ghidra.app.plugin.core.overview.addresstype;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import docking.ActionContext;
import docking.DialogComponentProvider;
import docking.action.DockingActionIf;
import generic.theme.GColor;
import ghidra.app.plugin.core.overview.*;
import ghidra.framework.model.*;
import ghidra.framework.options.OptionsChangeListener;
import ghidra.framework.options.ToolOptions;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSetView;
import ghidra.program.model.listing.*;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.symbol.Reference;
import ghidra.program.util.ProgramChangeRecord;
import ghidra.program.util.ProgramEvent;
import ghidra.util.HTMLUtilities;
import ghidra.util.HelpLocation;

/**
 * Service for associating colors with a programs addresses based on what program object is
 * at those addresses (functions, instructions, defined data, etc.)
 */
public class AddressTypeOverviewColorService
		implements OverviewColorService, OptionsChangeListener, DomainObjectListener {
	private static final String OPTIONS_NAME = "Overview";
	private static final GColor DEFAULT_INSTRUCTION_COLOR =
		new GColor("color.bg.plugin.overview.address.instruction");
	private static final GColor DEFAULT_DATA_COLOR =
		new GColor("color.bg.plugin.overview.address.data");
	private static final GColor DEFAULT_FUNCTION_COLOR =
		new GColor("color.bg.plugin.overview.address.function");
	private static final GColor DEFAULT_UNDEFINED_COLOR =
		new GColor("color.bg.plugin.overview.address.undefined");
	private static final GColor DEFAULT_UNINITIALIZED_COLOR =
		new GColor("color.bg.plugin.overview.address.uninitialized");
	private static final GColor DEFAULT_EXTERNAL_REF_COLOR =
		new GColor("color.bg.plugin.overview.address.external.ref");

	Color instructionColor = DEFAULT_INSTRUCTION_COLOR;
	Color dataColor = DEFAULT_DATA_COLOR;
	Color functionColor = DEFAULT_FUNCTION_COLOR;
	Color undefinedColor = DEFAULT_UNDEFINED_COLOR;
	Color uninitializedColor = DEFAULT_UNINITIALIZED_COLOR;
	Color externalRefColor = DEFAULT_EXTERNAL_REF_COLOR;

	private Program program;
	private Listing listing;
	private OverviewColorComponent overviewComponent;
	private PluginTool tool;
	private AddressTypeOverviewLegendPanel legendPanel;

	@Override
	public String getName() {
		return "Overview";
	}

	@Override
	public HelpLocation getHelpLocation() {
		return new HelpLocation(OverviewColorPlugin.HELP_TOPIC, "AddressTypeOverviewBar");
	}

	@Override
	public Color getColor(Address address) {
		AddressType type = getAddressType(address);
		return getColor(type);
	}

	@Override
	public String getToolTipText(Address address) {
		if (address == null) {
			return "";
		}
		AddressType addressType = getAddressType(address);
		StringBuffer buffer = new StringBuffer();
		buffer.append("<b>");
		buffer.append(HTMLUtilities.escapeHTML(getName()));
		buffer.append("</b>\n");
		buffer.append(addressType.getDescription());
		buffer.append(" (");
		buffer.append(HTMLUtilities.escapeHTML(getBlockName(address)));
		buffer.append(" ");
		buffer.append(address);
		buffer.append(" )");
		return HTMLUtilities.toWrappedHTML(buffer.toString(), 0);
	}

	@Override
	public List<DockingActionIf> getActions() {
		List<DockingActionIf> actions = new ArrayList<>();
		actions.add(new AbstractColorOverviewAction("Show Legend", getName(), overviewComponent,
			getHelpLocation()) {

			@Override
			public void actionPerformed(ActionContext context) {
				tool.showDialog(getLegendDialog());
			}
		});
		return actions;
	}

	@Override
	public void setProgram(Program program) {
		if (this.program != null) {
			this.program.removeListener(this);
		}
		this.program = program;
		this.listing = program == null ? null : program.getListing();

		if (this.program != null) {
			this.program.addListener(this);
		}
	}

	@Override
	public void initialize(PluginTool pluginTool) {
		this.tool = pluginTool;
		registerOptions();
		readOptions();
	}

	@Override
	public void setOverviewComponent(OverviewColorComponent component) {
		this.overviewComponent = component;
	}

	/**
	 * Returns the color associated with the given {@link AddressType}
	 *
	 * @param addressType the address type for which to get a color.
	 * @return the color associated with the given {@link AddressType}
	 */
	public Color getColor(AddressType addressType) {
		switch (addressType) {
			case FUNCTION:
				return functionColor;
			case UNINITIALIZED:
				return uninitializedColor;
			case EXTERNAL_REF:
				return externalRefColor;
			case INSTRUCTION:
				return instructionColor;
			case DATA:
				return dataColor;
			case UNDEFINED:
				return undefinedColor;
			default:
				return uninitializedColor;
		}
	}

	/**
	 * Determines the {@link AddressType} for the given address
	 *
	 * @param address the address for which to get an AddressType.
	 * @return the {@link AddressType} for the given address.
	 */
	public AddressType getAddressType(Address address) {
		if (listing == null || address == null) {
			return AddressType.UNINITIALIZED;
		}

		if (isInFunction(address)) {
			return AddressType.FUNCTION;
		}

		CodeUnit codeUnit = listing.getCodeUnitContaining(address);

		if (codeUnit == null) {
			return AddressType.UNINITIALIZED;
		}

		if (hasExternalReference(codeUnit)) {
			return AddressType.EXTERNAL_REF;
		}

		if (codeUnit instanceof Instruction) {
			return AddressType.INSTRUCTION;
		}

		if (codeUnit instanceof Data && ((Data) codeUnit).isDefined()) {
			return AddressType.DATA;
		}

		if (isInInitializedBlock(address)) {
			return AddressType.UNDEFINED;
		}

		return AddressType.UNINITIALIZED;
	}

	/**
	 * Sets the color to be associated with a given {@link AddressType}
	 *
	 * @param type the AddressType for which to assign the color.
	 * @param newColor the new color for the given {@link AddressType}
	 */
	public void setColor(AddressType type, Color newColor) {
		ToolOptions options = tool.getOptions(OPTIONS_NAME);
		switch (type) {
			case DATA:
				options.setColor("Data Color", newColor);
				break;
			case EXTERNAL_REF:
				options.setColor("External Reference Color", newColor);
				break;
			case FUNCTION:
				options.setColor("Function Color", newColor);
				break;
			case INSTRUCTION:
				options.setColor("Instruction Color", newColor);
				break;
			case UNDEFINED:
				options.setColor("Undefined Color", newColor);
				break;
			case UNINITIALIZED:
				options.setColor("Uninitialized Color", newColor);
				break;
			default:
				break;

		}
	}

	private boolean isInFunction(Address address) {
		return listing.getFunctionContaining(address) != null;
	}

	private boolean isInInitializedBlock(Address address) {
		MemoryBlock block = program.getMemory().getBlock(address);
		return block != null && block.isInitialized();
	}

	private boolean hasExternalReference(CodeUnit codeUnit) {
		Reference[] references =
			program.getReferenceManager().getReferencesFrom(codeUnit.getMinAddress());
		for (Reference ref : references) {
			if (ref.isExternalReference()) {
				return true;
			}
		}
		return false;

	}

	private void readOptions() {
		ToolOptions options = tool.getOptions(OPTIONS_NAME);
		options.addOptionsChangeListener(this);
		instructionColor = options.getColor("Instruction Color", DEFAULT_INSTRUCTION_COLOR);
		dataColor = options.getColor("Data Color", DEFAULT_DATA_COLOR);
		functionColor = options.getColor("Function Color", DEFAULT_FUNCTION_COLOR);
		undefinedColor = options.getColor("Undefined Color", DEFAULT_UNDEFINED_COLOR);
		uninitializedColor = options.getColor("Uninitialized Color", DEFAULT_UNINITIALIZED_COLOR);
		externalRefColor = options.getColor("External Reference Color", DEFAULT_EXTERNAL_REF_COLOR);
	}

	private void registerOptions() {
		ToolOptions options = tool.getOptions(OPTIONS_NAME);
		HelpLocation help = new HelpLocation(OverviewColorPlugin.HELP_TOPIC, "OverviewOptions");

		options.registerThemeColorBinding("Instruction Color", DEFAULT_INSTRUCTION_COLOR.getId(),
			help, "Color for instructions");
		options.registerThemeColorBinding("Data Color", DEFAULT_DATA_COLOR.getId(), help,
			"Color for data");
		options.registerThemeColorBinding("Function Color", DEFAULT_FUNCTION_COLOR.getId(), help,
			"Color for functions");
		options.registerThemeColorBinding("Undefined Color", DEFAULT_UNDEFINED_COLOR.getId(), help,
			"Color for undefined bytes");
		options.registerThemeColorBinding("Uninitialized Color",
			DEFAULT_UNINITIALIZED_COLOR.getId(), help, "Color for uninitialize memory");
		options.registerThemeColorBinding("External Reference Color",
			DEFAULT_EXTERNAL_REF_COLOR.getId(), help, "Color for external references");
	}

	@Override
	public void optionsChanged(ToolOptions options, String optionName, Object oldValue,
			Object newValue) {
		readOptions();
		if (overviewComponent != null) {
			overviewComponent.refreshAll();
		}
		if (legendPanel != null) {
			legendPanel.updateColors();
		}
	}

	@Override
	public void domainObjectChanged(DomainObjectChangedEvent ev) {
		for (int i = 0; i < ev.numRecords(); i++) {
			DomainObjectChangeRecord doRecord = ev.getChangeRecord(i);
			EventType eventType = doRecord.getEventType();

			if (eventType == ProgramEvent.FUNCTION_ADDED) {
				ProgramChangeRecord record = (ProgramChangeRecord) doRecord;
				Function function = (Function) record.getObject();
				AddressSetView addresses = function.getBody();
				overviewComponent.refresh(addresses.getMinAddress(), addresses.getMaxAddress());
			}

			else if (eventType == ProgramEvent.FUNCTION_REMOVED) {
				AddressSetView addresses = (AddressSetView) doRecord.getOldValue();
				overviewComponent.refresh(addresses.getMinAddress(), addresses.getMaxAddress());
			}

			else if (doRecord instanceof ProgramChangeRecord) {
				ProgramChangeRecord record = (ProgramChangeRecord) doRecord;
				overviewComponent.refresh(record.getStart(), record.getEnd());
			}
		}

	}

	private DialogComponentProvider getLegendDialog() {
		if (legendPanel == null) {
			legendPanel = new AddressTypeOverviewLegendPanel(this);
		}

		OverviewColorLegendDialog legendDialog =
			new OverviewColorLegendDialog("Overview Legend", legendPanel, getHelpLocation());
		return legendDialog;
	}

	private String getBlockName(Address address) {
		MemoryBlock block = program.getMemory().getBlock(address);
		if (block != null) {
			return block.getName();
		}
		return "";
	}

	@Override
	public Program getProgram() {
		return program;
	}

}
