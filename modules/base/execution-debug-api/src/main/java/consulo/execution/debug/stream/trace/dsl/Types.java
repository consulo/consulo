// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.impl.handler.type.ArrayType;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.trace.impl.handler.type.ListType;
import consulo.execution.debug.stream.trace.impl.handler.type.MapType;
import jakarta.annotation.Nonnull;

import java.util.function.Function;

/**
 * @author Vitaliy.Bibaev
 */
public interface Types {
  @Nonnull
  GenericType ANY();

  @Nonnull
  GenericType INT();

  @Nonnull
  GenericType LONG();

  @Nonnull
  GenericType BOOLEAN();

  @Nonnull
  GenericType DOUBLE();

  @Nonnull
  GenericType STRING();

  @Nonnull
  GenericType EXCEPTION();

  @Nonnull
  GenericType VOID();

  @Nonnull
  GenericType TIME();

  @Nonnull
  ArrayType array(@Nonnull GenericType elementType);

  @Nonnull
  ListType list(@Nonnull GenericType elementsType);

  @Nonnull
  MapType map(@Nonnull GenericType keyType, @Nonnull GenericType valueType);

  @Nonnull
  MapType linkedMap(@Nonnull GenericType keyType, @Nonnull GenericType valueType);

  @Nonnull
  GenericType nullable(@Nonnull Function<Types, GenericType> typeSelector);
}
