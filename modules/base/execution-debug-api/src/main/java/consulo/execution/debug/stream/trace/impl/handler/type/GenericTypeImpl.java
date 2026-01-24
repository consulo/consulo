// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.handler.type;

import java.util.Objects;

/**
 * @author Vitaliy.Bibaev
 */
public class GenericTypeImpl implements GenericType {
  private final String variableTypeName;
  private final String genericTypeName;
  private final String defaultValue;

  public GenericTypeImpl(String variableTypeName, String genericTypeName, String defaultValue) {
    this.variableTypeName = variableTypeName;
    this.genericTypeName = genericTypeName;
    this.defaultValue = defaultValue;
  }

  @Override
  public String getVariableTypeName() {
    return variableTypeName;
  }

  @Override
  public String getGenericTypeName() {
    return genericTypeName;
  }

  @Override
  public String getDefaultValue() {
    return defaultValue;
  }

  @Override
  public int hashCode() {
    return Objects.hash(variableTypeName, genericTypeName);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    return other instanceof GenericType && variableTypeName.equals(((GenericType)other).getVariableTypeName()) && genericTypeName.equals(((GenericType)other).getGenericTypeName());
  }

  @Override
  public String toString() {
    return "variable: " + variableTypeName + ", generic: " + genericTypeName;
  }
}
