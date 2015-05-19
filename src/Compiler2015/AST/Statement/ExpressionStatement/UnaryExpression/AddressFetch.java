package Compiler2015.AST.Statement.ExpressionStatement.UnaryExpression;

import Compiler2015.AST.Statement.ExpressionStatement.Expression;
import Compiler2015.AST.Statement.ExpressionStatement.IdentifierExpression;
import Compiler2015.Environment.Environment;
import Compiler2015.Environment.SymbolTableEntry;
import Compiler2015.Exception.CompilationError;
import Compiler2015.IR.CFG.ExpressionCFGBuilder;
import Compiler2015.IR.IRRegister.VirtualRegister;
import Compiler2015.IR.Instruction.Arithmetic.GlobalAddressFetch;
import Compiler2015.IR.Instruction.Arithmetic.LocalAddressFetch;
import Compiler2015.IR.Instruction.Move;
import Compiler2015.Type.VariablePointerType;
import Compiler2015.Type.VoidType;

/**
 * &e
 */
public class AddressFetch extends UnaryExpression {
	public AddressFetch(Expression e) {
		super(e);
		this.type = new VariablePointerType(e.type);
	}

	public static Expression getExpression(Expression e) {
		if (!e.isLValue)
			throw new CompilationError("Not LValue.");
		if (e.type instanceof VoidType)
			throw new CompilationError("Type Error");
		return new AddressFetch(e);
	}

	@Override
	public String getOperator() {
		return "&";
	}

	@Override
	public void emitCFG(ExpressionCFGBuilder builder) {
		e.emitCFG(builder);
		e.eliminateArrayRegister(builder);
		tempRegister = Environment.getTemporaryRegister();
		if (e instanceof IdentifierExpression) {
			int uId = ((IdentifierExpression) e).uId;
			SymbolTableEntry e = Environment.symbolNames.table.get(uId);
			if (e.scope == 1) {
				builder.addInstruction(new GlobalAddressFetch((VirtualRegister) tempRegister, uId));
			} else {
				builder.addInstruction(new LocalAddressFetch((VirtualRegister) tempRegister, uId));
			}
		} else {
			builder.addInstruction(new Move((VirtualRegister) tempRegister, e.tempRegister));
		}
	}
}
