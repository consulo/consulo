// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.types;

import consulo.language.dataFlow.value.RelationType;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a type that contains exactly one value
 */
public abstract class DfConstantType<T> implements DfType {
  private final T myValue;

  protected DfConstantType(T value) {
    myValue = value;
  }

  @Override
  public boolean isSuperType(DfType other) {
    return (other instanceof DfConstantType<?> constant && Objects.equals(myValue, constant.myValue)) || 
           other == DfType.BOTTOM;
  }

  @Override
  public DfType meet(DfType other) {
    return other.isSuperType(this) ? this : DfType.BOTTOM;
  }

  @Override
  public DfType fromRelation(RelationType relationType) {
    if (relationType == RelationType.EQ || relationType == RelationType.IS) return this;
    if (relationType == RelationType.NE || relationType == RelationType.IS_NOT) {
      DfType negated = tryNegate();
      if (negated != null) {
        return negated;
      }
    }
    return TOP;
  }

  /**
   * @return a value representation (different constants have different representation)
   */
  public T getValue() {
    return myValue;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(myValue);
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this ||
           obj != null && obj.getClass() == getClass() && Objects.equals(((DfConstantType<?>)obj).myValue, myValue);
  }

  @Override
  public String toString() {
    return String.valueOf(myValue);
  }

  @Override
  public boolean isConst(@Nullable Object constant) {
    return Objects.equals(myValue, constant);
  }

  @Override
  public <C> @Nullable C getConstantOfType(Class<C> clazz) {
    return ObjectUtil.tryCast(myValue, clazz);
  }
}
