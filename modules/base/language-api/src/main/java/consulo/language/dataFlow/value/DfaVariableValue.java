// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package consulo.language.dataFlow.value;

import consulo.language.dataFlow.types.DfType;
import consulo.util.lang.Pair;
import consulo.language.psi.PsiElement;
import consulo.util.collection.SmartList;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public final class DfaVariableValue extends DfaValue {

  public static class Factory {
    private final Map<Pair<VariableDescriptor, DfaVariableValue>, DfaVariableValue> myExistingVars = new HashMap<>();
    private final DfaValueFactory myFactory;

    Factory(DfaValueFactory factory) {
      myFactory = factory;
    }

    public DfaVariableValue createVariableValue(VariableDescriptor descriptor) {
      return createVariableValue(descriptor, null);
    }

    public DfaVariableValue createVariableValue(VariableDescriptor descriptor, @Nullable DfaVariableValue qualifier) {
      return createVariableValue(descriptor, qualifier, VariableDescriptor::getDfType);
    }

    /**
     * @param descriptor variable descriptor
     * @param qualifier qualifier (if present)
     * @return existing variable value with given descriptor and qualifier; null if it doesn't exist yet
     */
    public @Nullable DfaVariableValue getVariableValue(VariableDescriptor descriptor, @Nullable DfaVariableValue qualifier) {
      Pair<VariableDescriptor, DfaVariableValue> key = Pair.create(descriptor, qualifier);
      return myExistingVars.get(key);
    }

    DfaVariableValue createVariableValue(
      VariableDescriptor descriptor,
      @Nullable DfaVariableValue qualifier,
      BiFunction<? super VariableDescriptor, ? super @Nullable DfaVariableValue, ? extends DfType> typeSupplier) {
      Pair<VariableDescriptor, DfaVariableValue> key = Pair.create(descriptor, qualifier);
      DfaVariableValue var = myExistingVars.get(key);
      if (var == null) {
        var = new DfaVariableValue(descriptor, myFactory, qualifier, typeSupplier.apply(descriptor, qualifier));
        myExistingVars.put(key, var);
        while (qualifier != null) {
          qualifier.myDependents.add(var);
          qualifier = qualifier.getQualifier();
        }
      }
      return var;
    }
  }

  private final VariableDescriptor myDescriptor;
  private final DfType myDfType;
  private final @Nullable DfaVariableValue myQualifier;
  private DfType myInherentType;
  private final List<DfaVariableValue> myDependents = new SmartList<>();

  private DfaVariableValue(VariableDescriptor descriptor,
                           DfaValueFactory factory,
                           @Nullable DfaVariableValue qualifier,
                           DfType type) {
    super(factory);
    myDescriptor = descriptor;
    myQualifier = qualifier;
    myDfType = type;
  }

  public @Nullable PsiElement getPsiVariable() {
    return myDescriptor.getPsiElement();
  }

  public VariableDescriptor getDescriptor() {
    return myDescriptor;
  }

  @Override
  public DfaVariableValue bindToFactory(DfaValueFactory factory) {
    return factory.getVarFactory().createVariableValue(myDescriptor, myQualifier == null ? null : myQualifier.bindToFactory(factory),
                                                       (descriptor, value) -> myDfType);
  }

  @Override
  public DfType getDfType() {
    return myDfType;
  }

  @Override
  public boolean dependsOn(DfaVariableValue other) {
    return other == this || (myQualifier != null && myQualifier.dependsOn(other));
  }

  /**
   * @return list of all variables created within the same factory which are directly or indirectly qualified by this variable.
   */
  public List<DfaVariableValue> getDependentVariables() {
    return myDependents;
  }

  public int getDepth() {
    int depth = 0;
    DfaVariableValue qualifier = getQualifier();
    while (qualifier != null) {
      depth++;
      qualifier = qualifier.getQualifier();
    }
    return depth;
  }

  public DfaVariableValue withQualifier(DfaVariableValue newQualifier) {
    return newQualifier == myQualifier ? this : myFactory.getVarFactory().createVariableValue(myDescriptor, newQualifier);
  }

  @Override
  public String toString() {
    return (myQualifier == null ? "" : myQualifier + ".") + myDescriptor;
  }

  public @Nullable DfaVariableValue getQualifier() {
    return myQualifier;
  }

  public DfType getInherentType() {
    if(myInherentType == null) {
      myInherentType = myDescriptor.getInitialDfType(this, getFactory().getContext());
    }
    return myInherentType;
  }
  
  void resetInherentType() {
    myInherentType = null;
  }

  public boolean isFlushableByCalls() {
    return !myDescriptor.isStable() || (myQualifier != null && myQualifier.isFlushableByCalls());
  }

  /**
   * @return true if variable can be captured in closure
   */
  public boolean canBeCapturedInClosure() {
    return !myDescriptor.canBeCapturedInClosure() || (myQualifier != null && myQualifier.canBeCapturedInClosure());
  }

  public boolean containsCalls() {
    return myDescriptor.isCall() || myQualifier != null && myQualifier.containsCalls();
  }

  /**
   * @return false if the variable may be not equal to itself when applying the {@link RelationType#EQ}.
   * This could be possible if variable is backed by a pure method that always returns a new object. 
   * @param type current variable type (memory state-specific)
   */
  public boolean alwaysEqualsToItself(DfType type) {
    return myDescriptor.alwaysEqualsToItself(type);
  }
}
