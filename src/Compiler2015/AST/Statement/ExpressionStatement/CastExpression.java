package Compiler2015.AST.Statement.ExpressionStatement;

import Compiler2015.Exception.CompilationError;
import Compiler2015.Type.IntType;
import Compiler2015.Type.StructOrUnionType;
import Compiler2015.Type.Type;
import Compiler2015.Type.VoidType;

/**
 * (castTo)e
 */
public class CastExpression extends Expression {
	public Type castTo;
	public Expression e;

	public CastExpression(Type castTo, Expression e) {
		this.type = castTo;
		this.castTo = castTo;
		this.e = e;
	}

	public static Expression getExpression(Type t, Expression c) {
		if (t instanceof VoidType)
			throw new CompilationError("Cannot cast to void");
		if (t.equals(c.type))
			return c;
		if (t instanceof StructOrUnionType || c.type instanceof StructOrUnionType)
			throw new CompilationError("Cast failed");
		if (c instanceof CharConstant) {
			char v = ((CharConstant) c).c;
			return new IntConstant((int) v);
		}
		return new CastExpression(t, c);
	}

	public static boolean castable(Type from, Type to) {
		if (from.equals(to))
			return true;
		if (from instanceof StructOrUnionType || to instanceof StructOrUnionType)
			return false;
		return true;
	}

	public static Expression castToNumeric(Expression e) {
		if (!Type.isNumeric(e.type)) {
			if (CastExpression.castable(e.type, new IntType()))
				e = CastExpression.getExpression(new IntType(), e);
			else
				throw new CompilationError("Cannot cast to numeric types.");
		}
		return e;
	}
	@Override
	public String toString() {
		return String.format("(CastTo %s %s)", castTo, e);
	}
}