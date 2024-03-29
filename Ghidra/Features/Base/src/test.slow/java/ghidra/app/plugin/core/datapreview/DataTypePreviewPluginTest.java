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
/*
 * Created on Jun 7, 2006
 */
package ghidra.app.plugin.core.datapreview;

import static org.junit.Assert.*;

import org.junit.*;

import ghidra.app.plugin.core.codebrowser.CodeBrowserPlugin;
import ghidra.app.plugin.core.datamgr.DataTypeManagerPlugin;
import ghidra.app.plugin.core.datapreview.DataTypePreviewPlugin.DTPPTableModel;
import ghidra.app.services.GoToService;
import ghidra.program.database.ProgramBuilder;
import ghidra.program.model.address.Address;
import ghidra.program.model.data.*;
import ghidra.program.model.listing.Program;
import ghidra.test.AbstractGhidraHeadedIntegrationTest;
import ghidra.test.TestEnv;

public class DataTypePreviewPluginTest extends AbstractGhidraHeadedIntegrationTest {
	private TestEnv env;
	private DataTypePreviewPlugin plugin;

	@Before
	public void setUp() throws Exception {
		env = new TestEnv();

		env.getTool().addPlugin(CodeBrowserPlugin.class.getName());
		env.getTool().addPlugin(DataTypeManagerPlugin.class.getName());
		env.getTool().addPlugin(DataTypePreviewPlugin.class.getName());

		plugin = getPlugin(env.getTool(), DataTypePreviewPlugin.class);

		env.showTool();

		runSwing(() -> env.getTool().showComponentProvider(plugin.getProvider(), true));
	}

	@After
	public void tearDown() throws Exception {
		env.dispose();
	}

	@Test
	public void testPreview() throws Exception {

		DTPPTableModel model = plugin.getTableModel();
		GoToService gotoService = plugin.getGoToService();

		assertEquals(9, model.getRowCount());

		model.add(new ByteDataType());
		model.add(new CharDataType());
		model.add(new DoubleDataType());
		model.add(new QWordDataType());
		model.add(new TerminatedUnicodeDataType());

		assertEquals(9, model.getRowCount());

		Program program = buildProgram();

		env.open(program);

		gotoService.goTo(addr(program, 0x100df26));

		assertEquals("54h", model.getValueAt(0, DTPPTableModel.PREVIEW_COL));
		assertEquals("\"T\"", model.getValueAt(6, DTPPTableModel.PREVIEW_COL));
		assertEquals("20006500680054h", model.getValueAt(5, DTPPTableModel.PREVIEW_COL));
		assertEquals(
			"u\"The Margin values are not correct. Either they are not numeric characters " +
				"or they don't fit the dimensions of the page. Try either entering a number " +
				"or decreasing the margins.\",02h,00h,\"&f\\aPage &p\"",
			model.getValueAt(7, DTPPTableModel.PREVIEW_COL));

		gotoService.goTo(addr(program, 0x100e08c));

		assertEquals("50h", model.getValueAt(0, DTPPTableModel.PREVIEW_COL));
		assertEquals("'P'", model.getValueAt(1, DTPPTableModel.PREVIEW_COL));
		assertEquals("9.346009625593543E-307", model.getValueAt(2, DTPPTableModel.PREVIEW_COL));
		assertEquals("65006700610050h", model.getValueAt(5, DTPPTableModel.PREVIEW_COL));
		assertEquals("u\"Page &p\"", model.getValueAt(7, DTPPTableModel.PREVIEW_COL));

		Structure struct = new StructureDataType("TestStruct", 0);
		struct.add(new FloatDataType(), 4, "FLOAT", null);
		struct.add(new DoubleDataType(), 5, "DOUBLE", null);
		struct.add(new WordDataType(), 2, "WORD", null);
		struct.add(new DWordDataType(), 4, "DWORD", null);
		struct.add(new StringDataType(), 10, "STRING", null);

		int id = program.startTransaction("add");
		try {
			struct = (Structure) program.getDataTypeManager()
					.addDataType(struct,
						DataTypeConflictHandler.REPLACE_HANDLER);
		}
		finally {
			program.endTransaction(id, true);
		}
		model.add(struct);

		assertEquals("8.908155E-39", model.getValueAt(4, DTPPTableModel.PREVIEW_COL));
		assertEquals("6.119088925166103E-308", model.getValueAt(9, DTPPTableModel.PREVIEW_COL));
		assertEquals("2600h", model.getValueAt(10, DTPPTableModel.PREVIEW_COL));
		assertEquals("7000h", model.getValueAt(11, DTPPTableModel.PREVIEW_COL));
		assertEquals("00h,00h,00h,00h,00h,00h,00h,0Eh,00h,\"f\"",
			model.getValueAt(12, DTPPTableModel.PREVIEW_COL));

		assertEquals(14, model.getRowCount());
	}

	private Program buildProgram() throws Exception {
		ProgramBuilder builder = new ProgramBuilder("notepad", ProgramBuilder._X86);
		builder.createMemory("test1", "0x0100d000", 0x1000);
		builder.createMemory("test2", "0x0100e000", 0x1000);

		// p_unicode  "The Margin values are not correct. Either they are not numeric characters
		//             or they don't fit the dimensions of the page. Try either entering a
		//             number or decreasing the margins."
		builder.setBytes("0x100df24",
			"af 00 54 00 68 00 65 00 20 00 4d 00 61 00 72 00 " +
				"67 00 69 00 6e 00 20 00 76 00 61 00 6c 00 75 00 65 00 73 00 20 00 61 00 72 " +
				"00 65 00 20 00 6e 00 6f 00 74 00 20 00 63 00 6f 00 72 00 72 00 65 00 63 00 " +
				"74 00 2e 00 20 00 45 00 69 00 74 00 68 00 65 00 72 00 20 00 74 00 68 00 65 " +
				"00 79 00 20 00 61 00 72 00 65 00 20 00 6e 00 6f 00 74 00 20 00 6e 00 75 00 " +
				"6d 00 65 00 72 00 69 00 63 00 20 00 63 00 68 00 61 00 72 00 61 00 63 00 74 " +
				"00 65 00 72 00 73 00 20 00 6f 00 72 00 20 00 74 00 68 00 65 00 79 00 20 00 " +
				"64 00 6f 00 6e 00 27 00 74 00 20 00 66 00 69 00 74 00 20 00 74 00 68 00 65 " +
				"00 20 00 64 00 69 00 6d 00 65 00 6e 00 73 00 69 00 6f 00 6e 00 73 00 20 00 " +
				"6f 00 66 00 20 00 74 00 68 00 65 00 20 00 70 00 61 00 67 00 65 00 2e 00 20 " +
				"00 54 00 72 00 79 00 20 00 65 00 69 00 74 00 68 00 65 00 72 00 20 00 65 00 " +
				"6e 00 74 00 65 00 72 00 69 00 6e 00 67 00 20 00 61 00 20 00 6e 00 75 00 6d " +
				"00 62 00 65 00 72 00 20 00 6f 00 72 00 20 00 64 00 65 00 63 00 72 00 65 00 " +
				"61 00 73 00 69 00 6e 00 67 00 20 00 74 00 68 00 65 00 20 00 6d 00 61 00 72 " +
				"00 67 00 69 00 6e 00 73 00 2e 00");

		// p_unicode "&f"
		builder.setBytes("0x0100e084", "02 00 26 00 66 00");

		// p_unicode "Page &p"
		builder.setBytes("0x0100e08a", "07 00 50 00 61 00 67 00 65 00 20 00 26 00 70 00");

		builder.setBytes("0x0100e09a", "00 00 00 00 00 00 00 00 0e 00 66 00 46 00 70 00 " +
			"50 00 74 00 54 00 64 00 44 00 63 00 43 00 72 00 52 00 6c 00 4c 00");

		return builder.getProgram();
	}

	@Test
	public void testPreviewOrgChange() throws Exception {

		DTPPTableModel model = plugin.getTableModel();
		GoToService gotoService = plugin.getGoToService();

		model.removeAll();

		// Default size specified by DataOrganization
		//      shortSize		= 2;
		//      integerSize		= 4;
		//    	longSize		= 4;
		//		defaultAlignment = 1;
		//      alignment per size: 2->2, 4->4, 8->4

		plugin.addDataType(IntegerDataType.dataType);
		plugin.addDataType(LongDataType.dataType);
		plugin.addDataType(ShortDataType.dataType);

		Structure struct = new StructureDataType("test", 0);
		struct.setPackingEnabled(true);
		struct.add(IntegerDataType.dataType, "intField", "");
		struct.add(LongDataType.dataType, "longField", "");
		struct.add(ShortDataType.dataType, "shortField", "");

		plugin.addDataType(struct);

		assertEquals(6, model.getRowCount());

		Program program = buildProgram();
		DataOrganizationImpl dataOrganization =
			(DataOrganizationImpl) program.getCompilerSpec().getDataOrganization();
		dataOrganization.setLongSize(8);

		// Open program in tool and goto tested memory location
		env.open(program);

		gotoService.goTo(addr(program, 0x100df26));

// TODO: The below values have not been confirmed, since variation in DataOrganization is not yet supported by this plugin

		assertEquals("680054h", model.getValueAt(0, DTPPTableModel.PREVIEW_COL));// 4-byte int
		assertEquals("20006500680054h", model.getValueAt(1, DTPPTableModel.PREVIEW_COL));// 8-byte long
		assertEquals("54h", model.getValueAt(2, DTPPTableModel.PREVIEW_COL));// 2-byte short

		assertEquals("680054h", model.getValueAt(3, DTPPTableModel.PREVIEW_COL));// 4-byte int at offset 0
		assertEquals("6700720061004Dh", model.getValueAt(4, DTPPTableModel.PREVIEW_COL));// 8-byte long at offset 8
		assertEquals("69h", model.getValueAt(5, DTPPTableModel.PREVIEW_COL));// 2-byte short at offset 16

		env.close(program);

		// Re-create program with mutated data-organization to simulate shift to 3-byte aligned types
		// NOTE: Altering data organization on-the-fly is not supported

		// alignment map should jive with 3-byte mutliple primitive type sizes
		dataOrganization.clearSizeAlignmentMap();
		dataOrganization.setSizeAlignment(1, 1);
		dataOrganization.setSizeAlignment(3, 3);
		dataOrganization.setSizeAlignment(6, 6);
		dataOrganization.setShortSize(3);
		dataOrganization.setIntegerSize(3);
		dataOrganization.setLongSize(6);

		// Open program in tool and goto tested memory location
		program = buildProgram();
		env.open(program);

		gotoService.goTo(addr(program, 0x100df26));

		assertEquals("680054h", model.getValueAt(0, DTPPTableModel.PREVIEW_COL));// 3-byte int
		assertEquals("6500680054h", model.getValueAt(1, DTPPTableModel.PREVIEW_COL));// 6-byte long
		assertEquals("680054h", model.getValueAt(2, DTPPTableModel.PREVIEW_COL));// 3-byte short

		assertEquals("680054h", model.getValueAt(3, DTPPTableModel.PREVIEW_COL));// 3-byte int at offset 0
		assertEquals("61004D0020h", model.getValueAt(4, DTPPTableModel.PREVIEW_COL));// 6-byte long at offset 6
		assertEquals("670072h", model.getValueAt(5, DTPPTableModel.PREVIEW_COL));// 3-byte short at offset 12

	}

	private Address addr(Program prog, long offset) {
		return prog.getAddressFactory().getDefaultAddressSpace().getAddress(offset);
	}
}
