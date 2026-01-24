// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.handler.type;

import java.util.function.BiFunction;

/**
 * @author Vitaliy.Bibaev
 */
public class MapTypeImpl extends ClassTypeImpl implements MapType {
  private final GenericType keyType;
  private final GenericType valueType;
  private final GenericType entryType;

  public MapTypeImpl(GenericType keyType,
                     GenericType valueType,
                     BiFunction<String, String, String> toName,
                     String defaultValue,
                     BiFunction<String, String, String> toEntryType) {
    super(toName.apply(keyType.getGenericTypeName(), valueType.getGenericTypeName()), defaultValue);
    this.keyType = keyType;
    this.valueType = valueType;
    this.entryType = new ClassTypeImpl(toEntryType.apply(keyType.getGenericTypeName(), valueType.getGenericTypeName()));
  }

  @Override
  public GenericType getKeyType() {
    return keyType;
  }

  @Override
  public GenericType getValueType() {
    return valueType;
  }

  @Override
  public GenericType getEntryType() {
    return entryType;
  }
}
