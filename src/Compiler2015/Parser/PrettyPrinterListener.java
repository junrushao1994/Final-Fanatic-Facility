package Compiler2015.Parser;

import Compiler2015.AST.Statement.CompoundStatement;
import Compiler2015.Exception.CompilationError;
import Compiler2015.Utility.Panel;
import Compiler2015.Utility.Utility;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

class Config {
	public int indent = -1;
	public boolean wsL = false, wsR = false, nl = false;
}

public class PrettyPrinterListener extends Compiler2015BaseListener {
	public BufferedTokenStream tokenStream;
	public ArrayList<Config> configs;
	public int indent = 0;
	public int lastIndent;

	public PrettyPrinterListener(BufferedTokenStream tokenStream) {
		this.tokenStream = tokenStream;
		this.configs = new ArrayList<>();
	}

	public void ensureCapacity(int n) { // [0, n]
		while (configs.size() <= n)
			configs.add(new Config());
	}

	public void setWSL(int n) {
		ensureCapacity(n);
		configs.get(n).wsL = true;
	}

	public void setWSR(int n) {
		ensureCapacity(n);
		configs.get(n).wsR = true;
	}

	public void setIndent(int n, int indent) {
		ensureCapacity(n);
		configs.get(n).indent = indent;
	}

	public void setNL(int n) {
		ensureCapacity(n);
		configs.get(n).nl = true;
	}

	public void setNonNL(int n) {
		ensureCapacity(n);
		configs.get(n).nl = false;
	}

	public String convertComment(String s) {
		if (s.startsWith("/*"))
			return s;
		if (s.startsWith("//"))
			return "/*" + s.substring(2) + "*/";
		if (s.startsWith("#"))
			return s;
		throw new CompilationError("WTF");
	}

	public boolean endsWithNL(StringBuilder sb) {
		int n = sb.length(), m = Utility.NEW_LINE.length();
		if (n < m) return false;
		while (m > 0)
			if (sb.charAt(--n) != Utility.NEW_LINE.charAt(--m))
				return false;
		return true;
	}

	public void addWS(StringBuilder sb) {
		int len = sb.length();
		if (len == 0)
			return;
		if (sb.charAt(len - 1) == ' ' || sb.charAt(len - 1) == '\t' || endsWithNL(sb))
			return;
		sb.append(' ');
	}

	public void addIndent(StringBuilder sb, int d) {
		boolean wsFront = false;
		int len = sb.length();
		while (len >= 1 && sb.charAt(len - 1) == ' ') {
			sb.deleteCharAt(--len);
			wsFront = true;
		}
		if (wsFront) addNL(sb);
		sb.append(Utility.getIndent(d));
		lastIndent = d;
	}

	public void addNL(StringBuilder sb) {
		int len = sb.length();
		while (len >= 1 && sb.charAt(len - 1) == ' ')
			sb.deleteCharAt(--len);
		sb.append(Utility.NEW_LINE);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		List<Token> tokenList = tokenStream.getTokens();
		int n = tokenList.size() - 1;  // excluding EOF
		ensureCapacity(n);

		lastIndent = 0;
		for (int i = 0; i < n; ++i) {
			Token t = tokenList.get(i);
			if (t.getChannel() == Token.HIDDEN_CHANNEL) {
				if (!t.getText().startsWith("#")) {
					setWSL(i);
					setWSR(i);
				} else {
					setNL(i);
				}
			}
			Config c = configs.get(i);
			if (c.indent == -2) {
				if (Panel.prettyPrinterType.equals(Panel.krPrinter)) {
					addWS(sb);
					sb.append('{');
					addNL(sb);
				} else {
					addNL(sb);
					addIndent(sb, lastIndent);
					sb.append("{");
					addNL(sb);
				}
				continue;
			}
			if (c.indent >= 0) {
				addIndent(sb, c.indent);
			}
			if (c.wsL) {
				addWS(sb);
			}
			String text = t.getText().trim();
			if (t.getChannel() == Token.HIDDEN_CHANNEL) {
				sb.append(convertComment(text));
			} else {
				if (text.equals(";")) {
					int len = sb.length();
					while (len > 0 && sb.charAt(len - 1) == ' ')
						sb.deleteCharAt(--len);
				}
				sb.append(text);
			}
			if (c.wsR) {
				addWS(sb);
			}
			if (c.nl) {
				addNL(sb);
			}
		}
		return sb.toString();
	}

	@Override
	public void exitCompilationUnit(@NotNull Compiler2015Parser.CompilationUnitContext ctx) {
		for (Compiler2015Parser.FunctionDefinitionContext i : ctx.functionDefinition()) {
			Token t = i.getStop();
			setNL(t.getTokenIndex());
		}
		for (Compiler2015Parser.DeclarationContext i : ctx.declaration()) {
			Token t = i.getStop();
			setNL(t.getTokenIndex());
		}
		for (TerminalNode i : ctx.Semi()) {
			Token t = i.getSymbol();
			setNL(t.getTokenIndex());
		}
	}

	@Override
	public void exitDeclaration1(@NotNull Compiler2015Parser.Declaration1Context ctx) {
		setIndent(ctx.Typedef().getSymbol().getTokenIndex(), indent);
		setWSR(ctx.Typedef().getSymbol().getTokenIndex());
		setWSL(ctx.declaratorList().getStart().getTokenIndex());

		setNL(ctx.Semi().getSymbol().getTokenIndex());
	}

	@Override
	public void exitDeclaration2(@NotNull Compiler2015Parser.Declaration2Context ctx) {
		setIndent(ctx.typeSpecifier().getStart().getTokenIndex(), indent);
		setWSR(ctx.typeSpecifier().getStop().getTokenIndex());
		setNL(ctx.Semi().getSymbol().getTokenIndex());
	}

	@Override
	public void exitFunctionDefinition(@NotNull Compiler2015Parser.FunctionDefinitionContext ctx) {
		setIndent(ctx.typeSpecifier().getStart().getTokenIndex(), indent);
		setWSR(ctx.typeSpecifier().getStop().getTokenIndex());
	}

	@Override
	public void exitInitDeclaratorList(@NotNull Compiler2015Parser.InitDeclaratorListContext ctx) {
		for (TerminalNode i : ctx.Comma())
			setWSR(i.getSymbol().getTokenIndex());
	}

	@Override
	public void exitInitDeclarator(@NotNull Compiler2015Parser.InitDeclaratorContext ctx) {
		if (ctx.EQ() != null) {
			setWSL(ctx.EQ().getSymbol().getTokenIndex());
			setWSR(ctx.EQ().getSymbol().getTokenIndex());
		}
	}

	@Override
	public void enterStructOrUnionSpecifier1(@NotNull Compiler2015Parser.StructOrUnionSpecifier1Context ctx) {
		++indent;
	}

	@Override
	public void exitStructOrUnionSpecifier1(@NotNull Compiler2015Parser.StructOrUnionSpecifier1Context ctx) {
		--indent;
		setWSR(ctx.structOrUnion().getStop().getTokenIndex());
		if (ctx.Identifier() != null) {
			setWSL(ctx.Identifier().getSymbol().getTokenIndex());
			setWSR(ctx.Identifier().getSymbol().getTokenIndex());
		}
		setIndent(ctx.L3().getSymbol().getTokenIndex(), -2);
		setWSL(ctx.L3().getSymbol().getTokenIndex());
		setNL(ctx.L3().getSymbol().getTokenIndex());
		setIndent(ctx.R3().getSymbol().getTokenIndex(), indent);
		setWSR(ctx.R3().getSymbol().getTokenIndex());
	}

	@Override
	public void exitStructOrUnionSpecifier2(@NotNull Compiler2015Parser.StructOrUnionSpecifier2Context ctx) {
		setWSR(ctx.structOrUnion().getStop().getTokenIndex());

		if (ctx.Identifier() != null) {
			setWSL(ctx.Identifier().getSymbol().getTokenIndex());
			setWSR(ctx.Identifier().getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitStructDeclaration(@NotNull Compiler2015Parser.StructDeclarationContext ctx) {
		setIndent(ctx.typeSpecifier().getStart().getTokenIndex(), indent);
		setWSR(ctx.typeSpecifier().getStop().getTokenIndex());

		for (TerminalNode i : ctx.Comma()) {
			Token t = i.getSymbol();
			setWSR(t.getTokenIndex());
		}

		setNL(ctx.Semi().getSymbol().getTokenIndex());
	}

	@Override
	public void enterParameterTypeList(@NotNull Compiler2015Parser.ParameterTypeListContext ctx) {
		if (ctx.Comma() != null)
			setWSR(ctx.Comma().getSymbol().getTokenIndex());
	}

	@Override
	public void enterParameterList(@NotNull Compiler2015Parser.ParameterListContext ctx) {
		for (TerminalNode i : ctx.Comma()) {
			setWSR(i.getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitParameterDeclaration2(@NotNull Compiler2015Parser.ParameterDeclaration2Context ctx) {
		setWSR(ctx.typeSpecifier().getStop().getTokenIndex());
	}

	@Override
	public void exitParameterDeclaration3(@NotNull Compiler2015Parser.ParameterDeclaration3Context ctx) {
		setWSR(ctx.typeSpecifier().getStop().getTokenIndex());
	}

	@Override
	public void exitInitializer2(@NotNull Compiler2015Parser.Initializer2Context ctx) {
		for (TerminalNode i : ctx.Comma()) {
			setWSR(i.getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitExpressionStatement(@NotNull Compiler2015Parser.ExpressionStatementContext ctx) {
		setIndent(ctx.getStart().getTokenIndex(), indent);
		setNL(ctx.getStop().getTokenIndex());
	}

	@Override
	public void enterCompoundStatement(@NotNull Compiler2015Parser.CompoundStatementContext ctx) {
		++indent;
	}

	@Override
	public void exitCompoundStatement(@NotNull Compiler2015Parser.CompoundStatementContext ctx) {
		--indent;
		setIndent(ctx.L3().getSymbol().getTokenIndex(), -2);
		setWSL(ctx.L3().getSymbol().getTokenIndex());
		setNL(ctx.L3().getSymbol().getTokenIndex());
		setIndent(ctx.R3().getSymbol().getTokenIndex(), indent);
		setNL(ctx.R3().getSymbol().getTokenIndex());
	}

	@Override
	public void enterSelectionStatement(@NotNull Compiler2015Parser.SelectionStatementContext ctx) {
		setIndent(ctx.If().getSymbol().getTokenIndex(), indent);
		setWSR(ctx.If().getSymbol().getTokenIndex());
//		setWSR(ctx.R1().getSymbol().getTokenIndex());
		if (ctx.Else() != null) {
			setIndent(ctx.Else().getSymbol().getTokenIndex(), indent);
			setNL(ctx.Else().getSymbol().getTokenIndex());
		}
		if (!(ctx.s1 instanceof CompoundStatement)) {
			++indent;
			setNL(ctx.R1().getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitSelectionStatement(@NotNull Compiler2015Parser.SelectionStatementContext ctx) {
		if (!(ctx.s1 instanceof CompoundStatement))
			--indent;
	}

	@Override
	public void enterIterationStatement1(@NotNull Compiler2015Parser.IterationStatement1Context ctx) {
		if (!(ctx.whileS.a instanceof CompoundStatement))
			++indent;
	}

	@Override
	public void exitIterationStatement1(@NotNull Compiler2015Parser.IterationStatement1Context ctx) {
		if (!(ctx.whileS.a instanceof CompoundStatement)) {
			--indent;
			setNL(ctx.R1().getSymbol().getTokenIndex());
		}
		setIndent(ctx.While().getSymbol().getTokenIndex(), indent);
		setWSR(ctx.While().getSymbol().getTokenIndex());
	}

	@Override
	public void enterIterationStatement2(@NotNull Compiler2015Parser.IterationStatement2Context ctx) {
		setIndent(ctx.For().getSymbol().getTokenIndex(), indent);
		setWSR(ctx.For().getSymbol().getTokenIndex());
		for (TerminalNode i : ctx.Semi())
			setWSR(i.getSymbol().getTokenIndex());
		setWSR(ctx.R1().getSymbol().getTokenIndex());
		if (!(ctx.forS.d instanceof CompoundStatement)) {
			++indent;
			setNL(ctx.R1().getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitIterationStatement2(@NotNull Compiler2015Parser.IterationStatement2Context ctx) {
		if (!(ctx.forS.d instanceof CompoundStatement))
			--indent;
	}

	@Override
	public void exitJumpStatement1(@NotNull Compiler2015Parser.JumpStatement1Context ctx) {
		setIndent(ctx.getStart().getTokenIndex(), indent);
		setNL(ctx.getStop().getTokenIndex());
	}

	@Override
	public void exitJumpStatement2(@NotNull Compiler2015Parser.JumpStatement2Context ctx) {
		setIndent(ctx.getStart().getTokenIndex(), indent);
		setNL(ctx.getStop().getTokenIndex());
	}

	@Override
	public void exitJumpStatement3(@NotNull Compiler2015Parser.JumpStatement3Context ctx) {
		setIndent(ctx.getStart().getTokenIndex(), indent);
		if (ctx.expression() != null) {
			setWSL(ctx.expression().getStart().getTokenIndex());
		}
		setNL(ctx.getStop().getTokenIndex());
	}

	@Override
	public void exitExpression(@NotNull Compiler2015Parser.ExpressionContext ctx) {
		for (TerminalNode i : ctx.Comma())
			setWSR(i.getSymbol().getTokenIndex());
	}

	@Override
	public void exitAssignmentExpression2(@NotNull Compiler2015Parser.AssignmentExpression2Context ctx) {
		setWSL(ctx.assignmentOperator().getStart().getTokenIndex());
		setWSR(ctx.assignmentOperator().getStop().getTokenIndex());
	}

	@Override
	public void exitLogicalOrExpression(@NotNull Compiler2015Parser.LogicalOrExpressionContext ctx) {
		for (TerminalNode i : ctx.OrOr()) {
			setWSL(i.getSymbol().getTokenIndex());
			setWSR(i.getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitLogicalAndExpression(@NotNull Compiler2015Parser.LogicalAndExpressionContext ctx) {
		for (TerminalNode i : ctx.AndAnd()) {
			setWSL(i.getSymbol().getTokenIndex());
			setWSR(i.getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitInclusiveOrExpression(@NotNull Compiler2015Parser.InclusiveOrExpressionContext ctx) {
		for (TerminalNode i : ctx.Or()) {
			setWSL(i.getSymbol().getTokenIndex());
			setWSR(i.getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitExclusiveOrExpression(@NotNull Compiler2015Parser.ExclusiveOrExpressionContext ctx) {
		for (TerminalNode i : ctx.Caret()) {
			setWSL(i.getSymbol().getTokenIndex());
			setWSR(i.getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitAndExpression(@NotNull Compiler2015Parser.AndExpressionContext ctx) {
		for (TerminalNode i : ctx.And()) {
			setWSL(i.getSymbol().getTokenIndex());
			setWSR(i.getSymbol().getTokenIndex());
		}
	}

	@Override
	public void exitEqualityExpression(@NotNull Compiler2015Parser.EqualityExpressionContext ctx) {
		for (Compiler2015Parser.EqualityOperatorContext i : ctx.equalityOperator()) {
			setWSL(i.getStart().getTokenIndex());
			setWSR(i.getStop().getTokenIndex());
		}
	}

	@Override
	public void exitRelationalExpression(@NotNull Compiler2015Parser.RelationalExpressionContext ctx) {
		for (Compiler2015Parser.RelationalOperatorContext i : ctx.relationalOperator()) {
			setWSL(i.getStart().getTokenIndex());
			setWSR(i.getStop().getTokenIndex());
		}
	}

	@Override
	public void exitShiftExpression(@NotNull Compiler2015Parser.ShiftExpressionContext ctx) {
		for (Compiler2015Parser.ShiftOperatorContext i : ctx.shiftOperator()) {
			setWSL(i.getStart().getTokenIndex());
			setWSR(i.getStop().getTokenIndex());
		}
	}

	@Override
	public void exitAdditiveExpression(@NotNull Compiler2015Parser.AdditiveExpressionContext ctx) {
		for (Compiler2015Parser.AdditiveOperatorContext i : ctx.additiveOperator()) {
			setWSL(i.getStart().getTokenIndex());
			setWSR(i.getStop().getTokenIndex());
		}
	}

	@Override
	public void exitMultiplicativeExpression(@NotNull Compiler2015Parser.MultiplicativeExpressionContext ctx) {
		for (Compiler2015Parser.MultiplicativeOperatorContext i : ctx.multiplicativeOperator()) {
			setWSL(i.getStart().getTokenIndex());
			setWSR(i.getStop().getTokenIndex());
		}
	}

	@Override
	public void exitCastExpression2(@NotNull Compiler2015Parser.CastExpression2Context ctx) {
		setWSR(ctx.castExpression().getStop().getTokenIndex());
	}

	@Override
	public void exitTypeName2(@NotNull Compiler2015Parser.TypeName2Context ctx) {
		setWSR(ctx.typeSpecifier().getStop().getTokenIndex());
	}

	@Override
	public void exitUnaryExpression5(@NotNull Compiler2015Parser.UnaryExpression5Context ctx) {
		setWSR(ctx.SizeOf().getSymbol().getTokenIndex());
	}

	@Override
	public void exitArguments(@NotNull Compiler2015Parser.ArgumentsContext ctx) {
		for (TerminalNode i : ctx.Comma())
			setWSR(i.getSymbol().getTokenIndex());
	}

	@Override
	public void exitPrimaryExpression3(@NotNull Compiler2015Parser.PrimaryExpression3Context ctx) {
		List<TerminalNode> list = ctx.StringLiteral();
		int n = list.size();
		for (int i = 1; i < n; ++i)
			setWSL(list.get(i).getSymbol().getTokenIndex());
	}

	@Override
	public void enterLambdaExpression(Compiler2015Parser.LambdaExpressionContext ctx) {
		++indent;
	}

	@Override
	public void exitLambdaExpression(Compiler2015Parser.LambdaExpressionContext ctx) {
		setIndent(ctx.L2().getSymbol().getTokenIndex(), indent);
		setWSL(ctx.POINTER().getSymbol().getTokenIndex());
		setWSR(ctx.POINTER().getSymbol().getTokenIndex());
		setNonNL(ctx.compoundStatement.getStop().getTokenIndex());
		--indent;
	}
}
