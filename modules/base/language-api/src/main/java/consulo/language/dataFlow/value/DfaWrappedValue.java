// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.value;

import consulo.language.dataFlow.types.DfType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A reference value whose SpecialField value is equal to some variable.
 * Exists on the stack only.
 */
public final class DfaWrappedValue extends DfaValue {
  private final DfaVariableValue myWrappedValue;
  private final DerivedVariableDescriptor myDerivedVariableDescriptor;
  private final DfType myType;

  private DfaWrappedValue(DfaVariableValue valueToWrap,
                          DerivedVariableDescriptor field,
                          DfType type) {
    super(valueToWrap.getFactory());
    myWrappedValue = valueToWrap;
    myDerivedVariableDescriptor = field;
    myType = type;
  }

  @Override
  public String toString() {
    return myType + " [with " + myDerivedVariableDescriptor + "=" + myWrappedValue + "]";
  }

  public DfaVariableValue getWrappedValue() {
    return myWrappedValue;
  }

  public DerivedVariableDescriptor getSpecialField() {
    return myDerivedVariableDescriptor;
  }

  @Override
  public DfaValue bindToFactory(DfaValueFactory factory) {
    return factory.getWrapperFactory().createWrapper(myType, myDerivedVariableDescriptor, myWrappedValue.bindToFactory(factory));
  }

  @Override
  public boolean dependsOn(DfaVariableValue other) {
    return myWrappedValue.dependsOn(other);
  }

  @Override
  public DfType getDfType() {
    return myType;
  }

  public static class Factory {
    private final Map<List<?>, DfaWrappedValue> cachedValues = new HashMap<>();

    private final DfaValueFactory myFactory;

    public Factory(DfaValueFactory factory) {
      myFactory = factory;
    }

    public DfaValue createWrapper(DfType qualifierType, DerivedVariableDescriptor specialField, DfaValue specialFieldValue) {
      if (specialFieldValue instanceof DfaVariableValue && ((DfaVariableValue)specialFieldValue).getDescriptor() == specialField) {
        DfaVariableValue qualifier = ((DfaVariableValue)specialFieldValue).getQualifier();
        if (qualifier != null && qualifierType.isSuperType(qualifier.getDfType())) {
          return qualifier;
        }
      }
      if (specialFieldValue instanceof DfaTypeValue || specialFieldValue instanceof DfaWrappedValue) {
        DfType fieldValue = specialFieldValue.getDfType();
        DfType dfType = specialField.asDfType(qualifierType, fieldValue);
        return myFactory.fromDfType(dfType);
      }
      if (specialFieldValue instanceof DfaVariableValue) {
        return cachedValues.computeIfAbsent(Arrays.asList(specialFieldValue, specialField, qualifierType),
                                            k -> new DfaWrappedValue((DfaVariableValue)specialFieldValue, specialField, qualifierType));
      }
      return myFactory.fromDfType(qualifierType);
    }
  }
}
