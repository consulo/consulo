// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.memory;

import consulo.language.dataFlow.value.DfaValue;
import consulo.language.dataFlow.value.DfaValueFactory;
import consulo.language.dataFlow.value.DfaVariableValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

final class EqClassImpl extends SortedIntSet implements EqClass {
  private final DfaValueFactory myFactory;

  /**
   * A comparator which allows to select a "canonical" variable of several variables (which is minimal of them).
   * Variables with shorter qualifier chain are preferred to be canonical.
   */
  public static final Comparator<DfaVariableValue> CANONICAL_VARIABLE_COMPARATOR =
    Comparator.nullsFirst((v1, v2) -> {
      int result = EqClassImpl.CANONICAL_VARIABLE_COMPARATOR.compare(v1.getQualifier(), v2.getQualifier());
      if (result != 0) return result;
      return Integer.compare(v1.getID(), v2.getID());
    });

  EqClassImpl(DfaValueFactory factory) {
    myFactory = factory;
  }

  EqClassImpl(EqClassImpl toCopy) {
    super(toCopy.toNativeArray());
    myFactory = toCopy.myFactory;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("(");
    for (int i = 0; i < size(); i++) {
      if (i > 0) buf.append(", ");
      int value = get(i);
      DfaValue dfaValue = myFactory.getValue(value);
      buf.append(dfaValue);
    }
    buf.append(")");
    return buf.toString();
  }

  @Override
  public DfaVariableValue getVariable(int index) {
    return (DfaVariableValue)myFactory.getValue(get(index));
  }

  /**
   * @return copy of variables from this class as a list. Use this method if you expect
   * class updates during the iteration.
   */
  @Override
  public List<DfaVariableValue> asList() {
    List<DfaVariableValue> vars = new ArrayList<>(size());
    forValues(id -> vars.add((DfaVariableValue)myFactory.getValue(id)));
    return vars;
  }

  /**
   * @return the "canonical" variable for this class (according to {@link #CANONICAL_VARIABLE_COMPARATOR}) or
   * null if the class does not contain variables.
   */
  @Override
  public @Nullable DfaVariableValue getCanonicalVariable() {
    if (size() == 1) {
      return getVariable(0);
    }
    DfaVariableValue min = null;
    for (int i = 0; i < size(); i++) {
      DfaVariableValue variable = getVariable(i);
      if (min == null || CANONICAL_VARIABLE_COMPARATOR.compare(variable, min) < 0) {
        min = variable;
      }
    }
    return min;
  }

  @Override
  public Iterator<DfaVariableValue> iterator() {
    return new Iterator<>() {
      int pos;

      @Override
      public boolean hasNext() {
        return pos < size();
      }

      @Override
      public DfaVariableValue next() {
        if (pos >= size()) throw new NoSuchElementException();
        return (DfaVariableValue)myFactory.getValue(get(pos++));
      }
    };
  }

  @Override
  public boolean contains(DfaValue value) {
    return contains(value.getID());
  }

  @Override
  public boolean containsAll(EqClass that) {
    return containsAll((SortedIntSet)that);
  }
}
