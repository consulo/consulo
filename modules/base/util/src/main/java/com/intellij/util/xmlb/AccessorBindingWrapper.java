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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

class AccessorBindingWrapper extends Binding implements MultiNodeBinding {
  private final Binding myBinding;

  public AccessorBindingWrapper(@Nonnull MutableAccessor accessor, @Nonnull Binding binding) {
    super(accessor);

    myBinding = binding;
  }

  @Nullable
  @Override
  public Object serialize(@Nonnull Object o, @Nullable Object context, @Nonnull SerializationFilter filter) {
    Object value = myAccessor.read(o);
    if (value == null) {
      throw new XmlSerializationException("Property " + myAccessor + " of object " + o + " (" + o.getClass() + ") must not be null");
    }
    return myBinding.serialize(value, context, filter);
  }

  @Override
  @Nullable
  public Object deserialize(Object context, @Nonnull Element element) {
    Object currentValue = myAccessor.read(context);
    if (myBinding instanceof BeanBinding && myAccessor.isFinal()) {
      ((BeanBinding)myBinding).deserializeInto(currentValue, element, null);
    }
    else {
      Object deserializedValue = myBinding.deserialize(currentValue, element);
      if (currentValue != deserializedValue) {
        myAccessor.set(context, deserializedValue);
      }
    }
    return context;
  }

  @Nullable
  @Override
  public Object deserializeList(Object context, @Nonnull List<Element> elements) {
    Object currentValue = myAccessor.read(context);
    if (myBinding instanceof BeanBinding && myAccessor.isFinal()) {
      ((BeanBinding)myBinding).deserializeInto(currentValue, elements.get(0), null);
    }
    else {
      Object deserializedValue = Binding.deserializeList(myBinding, currentValue, elements);
      if (currentValue != deserializedValue) {
        myAccessor.set(context, deserializedValue);
      }
    }
    return context;
  }

  @Override
  public boolean isMulti() {
    return myBinding instanceof MultiNodeBinding && ((MultiNodeBinding)myBinding).isMulti();
  }

  @Override
  public boolean isBoundTo(@Nonnull Element element) {
    return myBinding.isBoundTo(element);
  }
}
