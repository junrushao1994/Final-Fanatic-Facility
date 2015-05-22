package Compiler2015.Translate.Naive.MIPS;

import Compiler2015.AST.Initializer;
import Compiler2015.AST.Statement.ExpressionStatement.Constant;
import Compiler2015.AST.Statement.ExpressionStatement.IdentifierExpression;
import Compiler2015.AST.Statement.ExpressionStatement.StringConstant;
import Compiler2015.Environment.Environment;
import Compiler2015.Environment.SymbolTableEntry;
import Compiler2015.Exception.CompilationError;
import Compiler2015.IR.CFG.CFGVertex;
import Compiler2015.IR.CFG.ControlFlowGraph;
import Compiler2015.IR.IRRegister.ArrayRegister;
import Compiler2015.IR.IRRegister.IRRegister;
import Compiler2015.IR.IRRegister.ImmediateValue;
import Compiler2015.IR.IRRegister.VirtualRegister;
import Compiler2015.IR.Instruction.*;
import Compiler2015.IR.Instruction.Arithmetic.*;
import Compiler2015.Type.*;
import Compiler2015.Utility.Tokens;
import Compiler2015.Utility.Utility;

import java.io.PrintWriter;
import java.util.ArrayList;

public final class Translator {

	public static String getFunctionLabel() {
		return String.format("___global___uId_%d___name_%s:",
				ControlFlowGraph.nowUId, Environment.symbolNames.table.get(ControlFlowGraph.nowUId).name);
	}

	public static String getFunctionLabelName(int uId) {
		return String.format("___global___uId_%d___name_%s",
				uId, Environment.symbolNames.table.get(uId).name);
	}

	public static String getGlobalVariableLabel(int uId) {
		return String.format("___global___uId_%d___name_%s:", uId, Environment.symbolNames.table.get(uId).name);
	}

	public static String getGlobalVariableLabelName(int uId) {
		return String.format("___global___uId_%d___name_%s", uId, Environment.symbolNames.table.get(uId).name);
	}

	public static String getStringConstantLabel(int uId) {
		return String.format("___global___uId_%d:", uId);
	}

	public static String getStringConstantLabelName(int uId) {
		return String.format("___global___uId_%d", uId);
	}

	public static String getVertexLabel(int id) {
		if (id < 0)
			return String.format("___function___uId_%d___vertex_neg_%d: ", ControlFlowGraph.nowUId, -id);
		return String.format("___function___uId_%d___vertex_%d: ", ControlFlowGraph.nowUId, id);
	}

	public static void generateGlobalVariables(PrintWriter out) {
		out.println(".data");
		for (SymbolTableEntry entry : Environment.symbolNames.table) {
			if (entry == null)
				continue;
			if (entry.scope == 1 && entry.type == Tokens.STRING_CONSTANT) {
				out.println(getStringConstantLabel(entry.uId));
				out.printf("\t.ascii \"%s\"", StringConstant.toPrintableString((String) entry.ref));
				out.println();
			}
			if (entry.scope != 1 || entry.type != Tokens.VARIABLE || entry.ref instanceof FunctionType)
				continue;
			Type type = (Type) entry.ref;
			int uId = entry.uId;
			if (type instanceof ArrayPointerType) {
				Type pointTo = ((ArrayPointerType) type).pointTo;
				if (entry.info == null) {
					out.println(getGlobalVariableLabel(uId));
					out.printf("\t.space %d", type.sizeof());
					out.println();
				} else {
					class ValuePairs {
						int position, value;

						public ValuePairs(int position, int value) {
							this.position = position;
							this.value = value;
						}
					}
					ArrayPointerType t = (ArrayPointerType) type;
					ArrayList<ValuePairs> pairs = new ArrayList<>();
					for (Initializer.InitEntry _entry : ((Initializer) entry.info).entries) {
						int pos = 0, mul = 1;
						for (int i = _entry.position.length - 1; i >= 0; --i) {
							pos += _entry.position[i] * mul;
							mul *= t.dimensions.get(i);
						}
						pairs.add(new ValuePairs(pos, Constant.toInt(_entry.value)));
					}
					pairs.sort((o1, o2) -> o1.position - o2.position);

					out.println(getGlobalVariableLabel(uId));
					int size = type.sizeof() / pointTo.sizeof();
					String prefix;
					if (pointTo.sizeof() == 1)
						prefix = "\t.byte ";
					else if (pointTo.sizeof() == 4)
						prefix = "\t.word ";
					else
						throw new CompilationError("Internal Error.");
					int pointer = 0;
					for (int i = 0; i < size; ++i) {
						if (pointer < pairs.size() && pairs.get(pointer).position == i) {
							out.printf("%s %d", prefix, pairs.get(pointer).value);
							out.println();
							++pointer;
						} else {
							out.printf("%s 0", prefix);
							out.println();
						}
					}
				}
			} else if (type instanceof CharType) {
				out.println(getGlobalVariableLabel(uId));
				Integer value = entry.info == null ? Integer.valueOf(0) : Constant.toInt(((Initializer) entry.info).entries.get(0).value);
				out.printf("\t.byte %d", value);
				out.println();
			} else if (type instanceof FunctionPointerType) {
				out.println(getGlobalVariableLabel(uId));
				out.printf("\t.word %d", entry.info == null ? 0 :
						Constant.toInt(((Initializer) entry.info).entries.get(0).value));
				out.println();
			} else if (type instanceof IntType) {
				out.println(getGlobalVariableLabel(uId));
				out.printf("\t.word %d", entry.info == null ? 0 :
						Constant.toInt(((Initializer) entry.info).entries.get(0).value));
				out.println();
			} else if (type instanceof StructOrUnionType) {
				out.println(getGlobalVariableLabel(uId));
				out.printf("\t.space %d", type.sizeof());
				out.println();
			} else if (type instanceof VariablePointerType) {
				if (type.equals(new VariablePointerType(CharType.instance)) && entry.info != null && ((Initializer) entry.info).entries.get(0).value instanceof StringConstant) {
					StringConstant sc = ((StringConstant) ((Initializer) entry.info).entries.get(0).value);
					out.println(getGlobalVariableLabel(uId));
					out.printf("\t.word %s", getStringConstantLabelName(sc.uId));
					out.println();
				} else {
					out.println(getGlobalVariableLabel(uId));
					Integer value = entry.info == null ? Integer.valueOf(0) : Constant.toInt(((Initializer) entry.info).entries.get(0).value);
					if (value != null)
						out.printf("\t.word %d", value);
					else if (((Initializer) entry.info).entries.get(0).value instanceof IdentifierExpression) {
						out.printf("\t.word %s", getGlobalVariableLabelName(((IdentifierExpression) ((Initializer) entry.info).entries.get(0).value).uId));
					} else
						throw new CompilationError("Not supported now.");
					out.println();
				}
			} else
				throw new CompilationError("Internal Error.");
		}
	}

	public static int getDelta(int uId) {
		if (ControlFlowGraph.tempDelta.containsKey(uId))
			return ControlFlowGraph.tempDelta.get(uId);
		if (ControlFlowGraph.parameterDelta.containsKey(uId))
			return ControlFlowGraph.parameterDelta.get(uId);
		throw new CompilationError("Internal Error.");
	}

	public static void loadAddressOfVariable(int uId, int reg, PrintWriter out) {
		if (uId <= 0)
			throw new CompilationError("Internal Error.");
		SymbolTableEntry e = Environment.symbolNames.table.get(uId);
		if (e.scope == 1) {
			if (e.type == Tokens.STRING_CONSTANT)
				out.printf("\tla $t%d, %s%s", reg, getStringConstantLabelName(uId), Utility.NEW_LINE);
			else if (e.type == Tokens.VARIABLE)
				out.printf("\tla $t%d, %s%s", reg, getGlobalVariableLabelName(uId), Utility.NEW_LINE);
			else
				throw new CompilationError("Internal Error.");
		} else {
			out.printf("\taddiu $t%d, $sp, %d%s", reg, getDelta(uId), Utility.NEW_LINE);
		}
	}

	public static void loadFromIRRegisterToTRegister(IRRegister from, int reg, PrintWriter out) {
		if (from instanceof ImmediateValue) {
			out.printf("\tli $t%d, %d%s", reg, ((ImmediateValue) from).a, Utility.NEW_LINE);
		} else if (from instanceof VirtualRegister) {
			int uId = from.getValue();
			if (uId <= 0)
				throw new CompilationError("Internal Error.");
			SymbolTableEntry e = Environment.symbolNames.table.get(uId);
			if (e.scope < 1)
				throw new CompilationError("Internal Error.");
			if (e.scope == 1) { // global variables
				if (e.type == Tokens.STRING_CONSTANT)
					out.printf("\tla $t%d, %s%s", reg, getStringConstantLabelName(uId), Utility.NEW_LINE);
				else if (e.type == Tokens.VARIABLE) {
					out.printf("\tla $t%d, %s%s", reg, getGlobalVariableLabelName(uId), Utility.NEW_LINE);
					if (!(e.ref instanceof FunctionType || e.ref instanceof ArrayPointerType || e.ref instanceof StructOrUnionType))
						out.printf("\tlw $t%d, 0($t%d)%s", reg, reg, Utility.NEW_LINE);
				} else
					throw new CompilationError("Internal Error.");
			} else { // local variables
				out.printf("\tlw $t%d, %d($sp)%s", reg, getDelta(uId), Utility.NEW_LINE);
			}
		} else if (from instanceof ArrayRegister) {
			loadFromIRRegisterToTRegister(((ArrayRegister) from).a, reg, out);
			String loadInstruction;
			if (((ArrayRegister) from).bitLen == 1)
				loadInstruction = "lb";
			else if (((ArrayRegister) from).bitLen == 4)
				loadInstruction = "lw";
			else
				throw new CompilationError("Internal Error.");
			out.printf("\t%s $t%d, %d($t%d)%s", loadInstruction, reg, ((ArrayRegister) from).b.a, reg, Utility.NEW_LINE);
		} else
			throw new CompilationError("Internal Error.");
	}

	public static void storeFromTRegisterToIRRegister(int reg, int uId, PrintWriter out) {
		out.printf("\tsw $t%d, %d($sp)%s", reg, getDelta(uId), Utility.NEW_LINE);
	}

	public static void storeFromPhysicalRegisterToIRRegister(String reg, int uId, PrintWriter out) {
		out.printf("\tsw %s, %d($sp)%s", reg, getDelta(uId), Utility.NEW_LINE);
	}

	public static void generateFunction(PrintWriter out) {
		ControlFlowGraph.scanVirtualRegister(); // calculate space of stack frame

		if (Environment.symbolNames.table.get(ControlFlowGraph.nowUId).name.equals("main"))
			out.println("main:");

		out.println(getFunctionLabel());
		out.println("\taddiu $sp, $sp, -" + ControlFlowGraph.frameSize);
		out.println("\tsw $sp, 0($sp)");
		out.println("\tsw $ra, 4($sp)");

		ArrayList<CFGVertex> sequence = Sequentializer.process();
		int numberOfArguments = 0;
		int numberOfExtraArguments = 0;
		for (CFGVertex vertex : sequence) {
			out.println(getVertexLabel(vertex.id));
			for (IRInstruction ins : vertex.internal) {
				out.println(Utility.NEW_LINE + "#\t" + ins);
				if (!(ins instanceof PushStack || ins instanceof Call || ins instanceof FetchReturn))
					numberOfArguments = numberOfExtraArguments = 0;

				if (ins instanceof AllocateHeap) {
					out.println("\tli $a0, " + ((AllocateHeap) ins).bitLen);
					out.println("\tli $v0, 9");
					out.println("\tsyscall");
					storeFromPhysicalRegisterToIRRegister("$v0", ins.getRd(), out);
				} else if (ins instanceof Call) {
					if (!(((Call) ins).func instanceof VirtualRegister))
						throw new CompilationError("Internal Error.");
					if (numberOfExtraArguments != 0)
						out.printf("\taddiu $sp, $sp, -%d%s", numberOfArguments * 4, Utility.NEW_LINE);
					VirtualRegister func = (VirtualRegister) ((Call) ins).func;
					if (Environment.symbolNames.table.get(func.uId).type == Tokens.VARIABLE) {
						out.printf("\tjal %s%s", getFunctionLabelName(func.uId), Utility.NEW_LINE);
					} else {
						loadFromIRRegisterToTRegister(func, 0, out);
						out.printf("\tjal $t0%s", Utility.NEW_LINE);
					}
					if (numberOfExtraArguments != 0)
						out.printf("\taddiu $sp, $sp, %d%s", numberOfArguments * 4, Utility.NEW_LINE);
				} else if (ins instanceof FetchReturn) {
					storeFromPhysicalRegisterToIRRegister("$v0", ins.getRd(), out);
				} else if (ins instanceof Move) {
					loadFromIRRegisterToTRegister(((Move) ins).rs, 0, out);
					storeFromTRegisterToIRRegister(0, ins.getRd(), out);
				} else if (ins instanceof Pop) {
					// do nothing
					out.print("");
				} else if (ins instanceof PushStack) {
					++numberOfArguments;
					if (((PushStack) ins).isExtra)
						++numberOfExtraArguments;
					if (((PushStack) ins).push instanceof VirtualRegister) {
						loadFromIRRegisterToTRegister(((PushStack) ins).push, 0, out);
						out.printf("\tsw $t0, -%d($sp)%s", numberOfArguments * 4, Utility.NEW_LINE);
					} else if (((PushStack) ins).push instanceof ImmediateValue) {
						out.printf("\tli $t0, %d%s", ((ImmediateValue) ((PushStack) ins).push).a, Utility.NEW_LINE);
						out.printf("\tsw $t0, -%d($sp)%s", numberOfArguments * 4, Utility.NEW_LINE);
					} else
						throw new CompilationError("Internal Error.");
				} else if (ins instanceof ReadArray) {
					loadFromIRRegisterToTRegister(((ReadArray) ins).rs, 0, out);
					storeFromTRegisterToIRRegister(0, ins.getRd(), out);
				} else if (ins instanceof WriteArray) {
					loadFromIRRegisterToTRegister(((WriteArray) ins).rs, 1, out);
					loadFromIRRegisterToTRegister(((WriteArray) ins).rd.a, 0, out);
					String loadInstruction;
					if (((WriteArray) ins).rd.bitLen == 1)
						loadInstruction = "sb";
					else if (((WriteArray) ins).rd.bitLen == 4)
						loadInstruction = "sw";
					else
						throw new CompilationError("Internal Error.");
					out.printf("\t%s $t1, %d($t0)%s", loadInstruction, ((WriteArray) ins).rd.b.a, Utility.NEW_LINE);
				} else if (ins instanceof SetReturn) {
					loadFromIRRegisterToTRegister(((SetReturn) ins).v0, 0, out);
					out.println("\tmove $v0, $t0");
				} else if (ins instanceof AddReg) {
					loadFromIRRegisterToTRegister(((AddReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((AddReg) ins).rt, 1, out);
					out.println("\tadd $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof BitwiseAndReg) {
					loadFromIRRegisterToTRegister(((BitwiseAndReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((BitwiseAndReg) ins).rt, 1, out);
					out.println("\tand $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof BitwiseNotReg) {
					loadFromIRRegisterToTRegister(((BitwiseNotReg) ins).rs, 0, out);
					out.println("\tnot $t1, $t0");
					storeFromTRegisterToIRRegister(1, ins.getRd(), out);
				} else if (ins instanceof BitwiseOrReg) {
					loadFromIRRegisterToTRegister(((BitwiseOrReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((BitwiseOrReg) ins).rt, 1, out);
					out.println("\tor $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof BitwiseXORReg) {
					loadFromIRRegisterToTRegister(((BitwiseXORReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((BitwiseXORReg) ins).rt, 1, out);
					out.println("\txor $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof DivideReg) {
					loadFromIRRegisterToTRegister(((DivideReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((DivideReg) ins).rt, 1, out);
					out.println("\tdiv $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof GlobalAddressFetch) {
					loadAddressOfVariable(((GlobalAddressFetch) ins).uId, 0, out);
					storeFromTRegisterToIRRegister(0, ((GlobalAddressFetch) ins).uId, out);
				} else if (ins instanceof LocalAddressFetch) {
					loadAddressOfVariable(((LocalAddressFetch) ins).uId, 0, out);
					storeFromTRegisterToIRRegister(0, ((LocalAddressFetch) ins).uId, out);
				} else if (ins instanceof ModuloReg) {
					loadFromIRRegisterToTRegister(((ModuloReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((ModuloReg) ins).rt, 1, out);
					out.println("\trem $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof MultiplyReg) {
					loadFromIRRegisterToTRegister(((MultiplyReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((MultiplyReg) ins).rt, 1, out);
					out.println("\tmul $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof NegateReg) {
					loadFromIRRegisterToTRegister(((NegateReg) ins).rs, 0, out);
					out.println("\tneg $t1, $t0");
					storeFromTRegisterToIRRegister(1, ins.getRd(), out);
				} else if (ins instanceof SetEqualTo) {
					loadFromIRRegisterToTRegister(((SetEqualTo) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((SetEqualTo) ins).rt, 1, out);
					out.println("\tseq $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof SetGE) {
					loadFromIRRegisterToTRegister(((SetGE) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((SetGE) ins).rt, 1, out);
					out.println("\tsge $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof SetGreaterThan) {
					loadFromIRRegisterToTRegister(((SetGreaterThan) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((SetGreaterThan) ins).rt, 1, out);
					out.println("\tsgt $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof SetLE) {
					loadFromIRRegisterToTRegister(((SetLE) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((SetLE) ins).rt, 1, out);
					out.println("\tsle $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof SetLessThan) {
					loadFromIRRegisterToTRegister(((SetLessThan) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((SetLessThan) ins).rt, 1, out);
					out.println("\tslt $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof SetNotEqual) {
					loadFromIRRegisterToTRegister(((SetNotEqual) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((SetNotEqual) ins).rt, 1, out);
					out.println("\tsne $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof ShiftLeftReg) {
					loadFromIRRegisterToTRegister(((ShiftLeftReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((ShiftLeftReg) ins).rt, 1, out);
					out.println("\tsll $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof ShiftRightReg) { // sra
					loadFromIRRegisterToTRegister(((ShiftRightReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((ShiftRightReg) ins).rt, 1, out);
					out.println("\tsra $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else if (ins instanceof SubtractReg) {
					loadFromIRRegisterToTRegister(((SubtractReg) ins).rs, 0, out);
					loadFromIRRegisterToTRegister(((SubtractReg) ins).rt, 1, out);
					out.println("\tsub $t2, $t0, $t1");
					storeFromTRegisterToIRRegister(2, ins.getRd(), out);
				} else
					throw new CompilationError("Internal Error.");
			}
		}

		out.println("\tlw $ra, 4($sp)");
		out.println("\tlw $sp, 0($sp)");
		out.println("\tjr $ra");
	}
}
