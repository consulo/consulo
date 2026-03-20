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
package consulo.util.xml.serializer.internal;

import consulo.util.xml.serializer.SerializationFilter;
import consulo.util.xml.serializer.XmlSerializationException;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

class AccessorBindingWrapper extends NonNullAccessorBinding implements MultiNodeBinding {
  private final Binding myBinding;

  public AccessorBindingWrapper(MutableAccessor accessor, Binding binding) {
    super(accessor);

    myBinding = binding;
  }

  @Override
  public @Nullable Object serialize(Object o, @Nullable Object context, SerializationFilter filter) {
    Object value = myAccessor.read(o);
    if (value == null) {
      throw new XmlSerializationException("Property " + myAccessor + " of object " + o + " (" + o.getClass() + ") must not be null");
    }
    return myBinding.serialize(value, context, filter);
  }

  @Override
  public @Nullable Object deserialize(@Nullable Object context, Element element) {
    Objects.requireNonNull(context);
    Object currentValue = myAccessor.read(context);
    if (myBinding instanceof BeanBinding beanBinding && myAccessor.isFinal()) {
      beanBinding.deserializeIntoObject(currentValue, element, null);
    }
    else {
      Object deserializedValue = myBinding.deserialize(currentValue, element);
      if (currentValue != deserializedValue) {
        myAccessor.set(context, deserializedValue);
      }
    }
    return context;
  }

  @Override
  public @Nullable Object deserializeList(Object context, List<Element> elements) {
    Object currentValue = myAccessor.read(context);
    if (myBinding instanceof BeanBinding beanBinding && myAccessor.isFinal()) {
      beanBinding.deserializeIntoObject(currentValue, elements.get(0), null);
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
  public boolean isBoundTo(Element element) {
    return myBinding.isBoundTo(element);
  }
}
