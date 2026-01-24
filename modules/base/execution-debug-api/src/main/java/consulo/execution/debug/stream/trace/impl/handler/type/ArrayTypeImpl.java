// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.handler.type;

import java.util.function.Function;

/**
 * @author Vitaliy.Bibaev
 */
public class ArrayTypeImpl extends ClassTypeImpl implements ArrayType {
  private final GenericType elementType;
  private final Function<String, String> toDefaultValue;

  public ArrayTypeImpl(GenericType elementType, Function<String, String> toName, Function<String, String> toDefaultValue) {
    super(toName.apply(elementType.getVariableTypeName()), toDefaultValue.apply("1"));
    this.elementType = elementType;
    this.toDefaultValue = toDefaultValue;
  }

  @Override
  public GenericType getElementType() {
    return elementType;
  }

  @Override
  public String sizedDeclaration(String size) {
    return toDefaultValue.apply(size);
  }
}
