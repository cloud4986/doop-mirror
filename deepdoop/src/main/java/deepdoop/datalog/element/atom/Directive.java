package deepdoop.datalog.element.atom;

import deepdoop.actions.IVisitable;
import deepdoop.actions.IVisitor;
import deepdoop.datalog.expr.ConstantExpr;
import deepdoop.datalog.expr.VariableExpr;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Directive implements IAtom {

	public final String       name;
	public final StubAtom     backtick;
	public final ConstantExpr constant;
	public final boolean      isPredicate;

	int                       _arity;

	public Directive(String name, StubAtom backtick) {
		assert backtick != null;
		this.name         = name;
		this.backtick     = backtick;
		this.constant     = null;
		this.isPredicate  = true;
		this._arity       = 1;
	}
	public Directive(String name, StubAtom backtick, ConstantExpr constant) {
		this.name         = name;
		this.backtick     = backtick;
		this.constant     = constant;
		this.isPredicate  = false;
		_arity            = (backtick == null ? 1 : 2);
	}

	@Override
	public IVisitable accept(IVisitor v) {
		v.enter(this);
		Map<IVisitable, IVisitable> m = new HashMap<>();
		if (backtick != null) m.put(backtick, backtick.accept(v));
		if (constant != null) m.put(constant, constant.accept(v));
		return v.exit(this, m);
	}

	@Override
	public String name() { return name; }
	@Override
	public String stage() { return null; }
	@Override
	public int arity() { return _arity; }
	@Override
	public List<VariableExpr> getVars() {
		return (_arity == 1 ? Arrays.asList((VariableExpr[])null) : Arrays.asList(null, null));
	}
	@Override
	public IAtom instantiate(String stage, List<VariableExpr> vars) { return this; }

	@Override
	public String toString() {
		String middle = (backtick != null ? "`" + backtick.name : "");
		if (isPredicate)
			return name + "(" + middle + ")";
		else
			return name + "[" + middle + "] = " + constant;
	}
}
