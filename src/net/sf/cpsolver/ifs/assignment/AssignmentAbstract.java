package net.sf.cpsolver.ifs.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.cpsolver.ifs.assignment.context.AssignmentContext;
import net.sf.cpsolver.ifs.assignment.context.AssignmentContextHolder;
import net.sf.cpsolver.ifs.assignment.context.AssignmentContextReference;
import net.sf.cpsolver.ifs.constant.ConstantVariable;
import net.sf.cpsolver.ifs.model.Constraint;
import net.sf.cpsolver.ifs.model.GlobalConstraint;
import net.sf.cpsolver.ifs.model.Model;
import net.sf.cpsolver.ifs.model.Value;
import net.sf.cpsolver.ifs.model.Variable;

/**
 * An abstract implementation of an {@link Assignment} object. It contains an instance of
 * a given assignment context holder (see {@link AssignmentContextHolder}) and 
 * implements the assignment logic. But the actual process of storing and retrieving values
 * is left on the assignment implementation.
 * 
 * @see Assignment
 * 
 * @version IFS 1.2 (Iterative Forward Search)<br>
 *          Copyright (C) 2014 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see <http://www.gnu.org/licenses/>.
 **/
public abstract class AssignmentAbstract<V extends Variable<V, T>, T extends Value<V, T>> implements Assignment<V, T> {
    protected AssignmentContextHolder<V, T> iContexts;
    
    /**
     * Constructor
     * @param contexts an instance of the assignment context holder
     */
    public AssignmentAbstract(AssignmentContextHolder<V, T> contexts) {
        iContexts = contexts;
    }
    
    /**
     * Checks if the variable is {@link ConstantVariable}, returns {@link AssignmentAbstract#getValueInternal(Variable)}
     * if the variable is not a constant.
     */
    @Override
    @SuppressWarnings("unchecked")
    public T getValue(V variable) {
        if (variable instanceof ConstantVariable<?> && ((ConstantVariable<?>)variable).isConstant())
            return ((ConstantVariable<T>)variable).getValue();
        return getValueInternal(variable);
    }

    /**
     * Returns assignment of a variable, null if not assigned. To be implemented.
     **/
    protected abstract T getValueInternal(V variable);
    
    /**
     * Sets an assignment to a variable (unassigns a variable if the given value is null). To be implemented.
     **/
    protected abstract void setValueInternal(long iteration, V variable, T value);
    
    /** Assigns a variable with the given value. All the appropriate classes are notified about the change.
     * It is using {@link AssignmentAbstract#setValueInternal(long, Variable, Value)} to store the new 
     * assignment.
     * @param variable a variable
     * @param value one of its values, null if the variable is to be unassigned
     * @return previous assignment of the variable 
     **/
    protected T assign(long iteration, V variable, T value) {
        assert variable.getModel() != null && (value == null || variable.equals(value.variable()));
        Model<V, T> model = variable.getModel();
        
        // unassign old value, if assigned
        T old = getValueInternal(variable);
        if (old != null) {
            model.beforeUnassigned(this, iteration, old);
            setValueInternal(iteration, variable, null);
            for (Constraint<V, T> constraint : variable.constraints())
                constraint.unassigned(this, iteration, old);
            for (GlobalConstraint<V, T> constraint : model.globalConstraints())
                constraint.unassigned(this, iteration, old);
            variable.variableUnassigned(this, iteration, old);
            model.afterUnassigned(this, iteration, old);
        }
        
        // assign new value, if provided
        if (value != null) {
            model.beforeAssigned(this, iteration, value);
            setValueInternal(iteration, variable, value);
            for (Constraint<V, T> constraint : variable.constraints())
                constraint.assigned(this, iteration, value);
            for (GlobalConstraint<V, T> constraint : model.globalConstraints())
                constraint.assigned(this, iteration, value);
            variable.variableAssigned(this, iteration, value);
            model.afterAssigned(this, iteration, value);
        }
        
        // return old value
        return old;
    }
    
    @Override
    public T assign(long iteration, T value) {
        return assign(iteration, value.variable(), value);
    }
    
    @Override
    public T unassign(long iteration, V variable) {
        return assign(iteration, variable, null);
    }
    
    @Override
    public int nrAssignedVariables() {
        return assignedVariables().size();
    }
    
    @Override
    public Collection<T> assignedValues() {
        List<T> values = new ArrayList<T>();
        for (V variable: assignedVariables())
            values.add(getValueInternal(variable));
        return values;
    }

    @Override
    public Collection<V> unassignedVariables(Model<V, T> model) {
        List<V> unassigned = new ArrayList<V>();
        for (V variable: model.variables())
            if (getValue(variable) == null)
                unassigned.add(variable);
        return unassigned;
    }

    @Override
    public int nrUnassignedVariables(Model<V, T> model) {
        return model.variables().size() - nrAssignedVariables();
    }

    @Override
    public <C extends AssignmentContext> C getAssignmentContext(AssignmentContextReference<V, T, C> reference) {
        return iContexts.getAssignmentContext(this, reference);
    }
    
    @Override
    public int getIndex() {
        return -1;
    }
}