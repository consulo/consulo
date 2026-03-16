// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.impl.handler.type.ArrayType;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.trace.impl.handler.type.ListType;
import consulo.execution.debug.stream.trace.impl.handler.type.MapType;

import java.util.function.Function;

/**
 * @author Vitaliy.Bibaev
 */
public interface Types {
  
  GenericType ANY();

  
  GenericType INT();

  
  GenericType LONG();

  
  GenericType BOOLEAN();

  
  GenericType DOUBLE();

  
  GenericType STRING();

  
  GenericType EXCEPTION();

  
  GenericType VOID();

  
  GenericType TIME();

  
  ArrayType array(GenericType elementType);

  
  ListType list(GenericType elementsType);

  
  MapType map(GenericType keyType, GenericType valueType);

  
  MapType linkedMap(GenericType keyType, GenericType valueType);

  
  GenericType nullable(Function<Types, GenericType> typeSelector);
}
