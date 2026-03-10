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

import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.SerializationFilter;
import consulo.util.xml.serializer.XmlSerializationException;
import consulo.util.xml.serializer.annotation.Tag;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class JDOMElementBinding extends Binding implements MultiNodeBinding {
  private final String myTagName;

  public JDOMElementBinding(MutableAccessor accessor) {
    super(accessor);

    Tag tag = accessor.getAnnotation(Tag.class);
    assert tag != null : "jdom.Element property without @Tag annotation: " + accessor;

    String tagName = tag.value();
    if (StringUtil.isEmpty(tagName)) {
      tagName = accessor.getName();
    }
    myTagName = tagName;
  }

  @Nullable
  @Override
  public Object serialize(Object o, @Nullable Object context, SerializationFilter filter) {
    assert myAccessor != null;
    Object value = myAccessor.read(o);
    if (value == null) {
      return null;
    }

    if (value instanceof Element element) {
      Element targetElement = element.clone();
      assert targetElement != null;
      targetElement.setName(myTagName);
      return targetElement;
    }
    if (value instanceof Element[] elements) {
      List<Element> result = new ArrayList<>();
      for (Element element : elements) {
        result.add(element.clone().setName(myTagName));
      }
      return result;
    }
    throw new XmlSerializationException("org.jdom.Element expected but " + value + " found");
  }

  @Nullable
  @Override
  public Object deserializeList(Object context, List<Element> elements) {
    assert myAccessor != null;
    if (myAccessor.getValueClass().isArray()) {
      myAccessor.set(context, elements.toArray(new Element[elements.size()]));
    }
    else {
      myAccessor.set(context, elements.get(0));
    }
    return context;
  }

  @Override
  public boolean isMulti() {
    return true;
  }

  @Override
  @Nullable
  public Object deserialize(@Nullable Object context, Element element) {
    assert myAccessor != null && context != null;
    myAccessor.set(context, element);
    return context;
  }

  @Override
  public boolean isBoundTo(Element element) {
    return element.getName().equals(myTagName);
  }
}
