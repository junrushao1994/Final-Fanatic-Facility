package Compiler2015.Parser;

import Compiler2015.AST.Statement.ExpressionStatement.Expression;
import Compiler2015.Exception.CompilationError;
import Compiler2015.Type.*;
import Compiler2015.Utility.Tokens;

import java.util.ArrayList;
import java.util.Stack;

class AnalyserEntry {
	Tokens type;
	Expression e = null;
	ArrayList<Type> types = null;
	ArrayList<String> names = null;
	boolean hasVaList = false;

	public AnalyserEntry(Tokens type, Expression e, ArrayList<Type> types) {
		this.type = type;
		this.e = e;
		this.types = types;
		this.hasVaList = false;
	}

	public AnalyserEntry(ArrayList<Type> types, ArrayList<String> names, boolean hasVaList) {
		this.type = Tokens.PARAMETER_BRACKET;
		this.e = null;
		this.types = types;
		this.names = names;
		this.hasVaList = hasVaList;
	}
}

/**
 * @see <a href="http://unixwiz.net/techtips/reading-cdecl.html">Reading C type declarations</a>
 * In fact, I just maintain a stack containing all operators and simply assemble them up.
 */
public final class TypeAnalyser {
	public static Stack<Type> ss = new Stack<>();
	public static Stack<Stack<AnalyserEntry>> se = new Stack<>();

	public static void enter(Type t) {
		ss.push(t);
		se.push(new Stack<>());
	}

	public static void exit() {
		ss.pop();
		se.pop();
	}

	public static void addStar() {
		se.peek().push(new AnalyserEntry(Tokens.STAR, null, null));
	}

	public static void addParameter(ArrayList<Type> types, ArrayList<String> names, boolean hasVaList) {
		if (types == null)
			types = new ArrayList<>();
		se.peek().push(new AnalyserEntry(types, names, hasVaList));
	}

	public static void addParameter() {
		se.peek().push(new AnalyserEntry(Tokens.PARAMETER_BRACKET, null, new ArrayList<>()));
	}

	public static void addArray(Expression e) {
		se.peek().push(new AnalyserEntry(Tokens.ARRAY_BRACKET, e, null));
	}

	public static Type analyse(boolean reserve) {
		Type ret = ss.peek();
		Stack<AnalyserEntry> stack = se.peek();
		while (!stack.isEmpty()) {
			AnalyserEntry e = stack.pop();
			if (e.type == Tokens.STAR) {
				if (ret instanceof FunctionType)
					ret = new FunctionPointerType((FunctionType) ret);
				else
					ret = new VariablePointerType(ret);
			} else if (e.type == Tokens.PARAMETER_BRACKET) {
				if (!reserve || !stack.isEmpty()) {
					ArrayList<String> emptyNames = new ArrayList<>(e.types.size());
					for (int i = 0; i < e.types.size(); ++i)
						emptyNames.add("");
					ret = new FunctionType(ret, e.types, emptyNames, e.hasVaList);
				} else {
					if (e.names == null)
						e.names = new ArrayList<>();
					ret = new FunctionType(ret, e.types, e.names, e.hasVaList);
				}
			} else if (e.type == Tokens.ARRAY_BRACKET) {
				if (e.e == null)
					ret = ArrayPointerType.pushFromUndimensioned(ret);
				else
					ret = ArrayPointerType.pushFrontDimension(ret, e.e);
			} else
				throw new CompilationError("Internal Error.");
		}
		return ret;
	}

	public static Type analyse() {
		return analyse(false);
	}
}
