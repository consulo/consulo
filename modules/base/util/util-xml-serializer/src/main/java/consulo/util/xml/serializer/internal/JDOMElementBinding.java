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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

class JDOMElementBinding extends Binding implements MultiNodeBinding {
  private final String myTagName;

  public JDOMElementBinding(@Nonnull MutableAccessor accessor) {
    super(accessor);

    Tag tag = myAccessor.getAnnotation(Tag.class);
    assert tag != null : "jdom.Element property without @Tag annotation: " + accessor;

    String tagName = tag.value();
    if (StringUtil.isEmpty(tagName)) {
      tagName = myAccessor.getName();
    }
    myTagName = tagName;
  }

  @Override
  public Object serialize(@Nonnull Object o, Object context, @Nonnull SerializationFilter filter) {
    Object value = myAccessor.read(o);
    if (value == null) {
      return null;
    }

    if (value instanceof Element) {
      Element targetElement = ((Element)value).clone();
      assert targetElement != null;
      targetElement.setName(myTagName);
      return targetElement;
    }
    if (value instanceof Element[]) {
      ArrayList<Element> result = new ArrayList<Element>();
      for (Element element : ((Element[])value)) {
        result.add(element.clone().setName(myTagName));
      }
      return result;
    }
    throw new XmlSerializationException("org.jdom.Element expected but " + value + " found");
  }

  @Nullable
  @Override
  public Object deserializeList(Object context, @Nonnull List<Element> elements) {
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
  public Object deserialize(Object context, @Nonnull Element element) {
    myAccessor.set(context, element);
    return context;
  }

  @Override
  public boolean isBoundTo(@Nonnull Element element) {
    return element.getName().equals(myTagName);
  }
}
