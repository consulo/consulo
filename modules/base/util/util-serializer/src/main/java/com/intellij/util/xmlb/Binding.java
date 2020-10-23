/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.xmlb;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.List;

abstract class Binding {
  static final Logger LOG = LoggerFactory.getLogger(Binding.class);

  protected final MutableAccessor myAccessor;

  protected Binding(MutableAccessor accessor) {
    myAccessor = accessor;
  }

  @Nonnull
  public MutableAccessor getAccessor() {
    return myAccessor;
  }

  @Nullable
  public abstract Object serialize(@Nonnull Object o, @Nullable Object context, @Nonnull SerializationFilter filter);

  @Nullable
  public Object deserialize(Object context, @Nonnull Element element) {
    return context;
  }

  public boolean isBoundTo(@Nonnull Element element) {
    return false;
  }

  void init(@Nonnull Type originalType) {
    // called (and make sense) only if MainBinding
  }

  @SuppressWarnings("CastToIncompatibleInterface")
  @Nullable
  public static Object deserializeList(@Nonnull Binding binding, Object context, @Nonnull List<Element> nodes) {
    if (binding instanceof MultiNodeBinding) {
      return ((MultiNodeBinding)binding).deserializeList(context, nodes);
    }
    else {
      if (nodes.size() == 1) {
        return binding.deserialize(context, nodes.get(0));
      }
      else if (nodes.isEmpty()) {
        return null;
      }
      else {
        throw new AssertionError("Duplicate data for " + binding + " will be ignored");
      }
    }
  }
}
