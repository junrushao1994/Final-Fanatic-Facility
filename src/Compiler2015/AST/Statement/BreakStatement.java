package Compiler2015.AST.Statement;

import Compiler2015.IR.CFG.ControlFlowGraph;
import Compiler2015.Utility.Utility;

/**
 * break;
 */
public class BreakStatement extends Statement {
	public Loop breakTo;

	public BreakStatement(Loop breakTo) {
		this.breakTo = breakTo;
	}

	@Override
	public String toString() {
		return "break;";
	}

	@Override
	public String deepToString(int depth) {
		return Utility.getIndent(depth).append(toString()).append(Utility.NEW_LINE).toString();
	}

	@Override
	public void emitCFG() {
		beginCFGBlock = endCFGBlock = ControlFlowGraph.instance.getNewVertex();
		endCFGBlock.unconditionalNext = breakTo.getOut();
	}

	@Override
	public Statement rebuild() {
		return this;
	}
}
