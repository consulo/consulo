// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.handler.type;

import java.util.function.Function;

/**
 * @author Vitaliy.Bibaev
 */
public class ListTypeImpl extends ClassTypeImpl implements ListType {
  private final GenericType elementType;

  public ListTypeImpl(GenericType elementType, Function<String, String> toName, String defaultValue) {
    super(toName.apply(elementType.getGenericTypeName()), defaultValue);
    this.elementType = elementType;
  }

  @Override
  public GenericType getElementType() {
    return elementType;
  }
}
