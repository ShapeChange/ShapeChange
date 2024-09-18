package de.interactive_instruments.shapechange.core.target.sql.structure;

import de.interactive_instruments.shapechange.core.target.sql.expressions.Expression;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class Select implements Statement {

	protected Expression expr = null;

	@Override
	public void accept(StatementVisitor visitor) {
		visitor.visit(this);
	}

	public Expression getExpression() {
		return expr;
	}

	public void setExpression(Expression expr) {
		this.expr = expr;
	}

	public boolean hasExpression() {
		return this.expr != null;
	}

}
