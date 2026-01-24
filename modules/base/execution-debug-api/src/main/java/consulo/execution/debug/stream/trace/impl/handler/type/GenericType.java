// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.handler.type;

/**
 * @author Vitaliy.Bibaev
 */
public interface GenericType {

  String getVariableTypeName();

  String getGenericTypeName();

  String getDefaultValue();

  interface CompositeType extends GenericType {
    GenericType getElementType();
  }
}
