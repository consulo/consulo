/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.util;

import consulo.logging.Logger;
import consulo.util.lang.reflect.ReflectionUtil;
import org.jdom.Element;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

// TODO [VISTALL] DO NOT CHANGE PACKAGE NAME. SOFT REF inside XmlSerializerImpl
@SuppressWarnings({"HardCodedStringLiteral"})
public class JDOMExternalizableStringList extends ArrayList<String> implements JDOMExternalizable {
  private static final Logger LOG = Logger.getInstance(JDOMExternalizableStringList.class);

  private static final String ATTR_LIST = "list";
  private static final String ATTR_LISTSIZE = "size";
  private static final String ATTR_ITEM = "item";
  private static final String ATTR_INDEX = "index";
  private static final String ATTR_CLASS = "class";
  private static final String ATTR_VALUE = "itemvalue";

  public JDOMExternalizableStringList(int initialCapacity) {
    super(initialCapacity);
  }

  public JDOMExternalizableStringList() {
  }

  public JDOMExternalizableStringList(@Nonnull Collection<? extends String> c) {
    super(c);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    clear();

    Class callerClass = null;
    for (Element listElement : element.getChildren(ATTR_LIST)) {
      if (callerClass == null) {
        callerClass = ReflectionUtil.findCallerClass(2);
        assert callerClass != null;
      }
      final ClassLoader classLoader = callerClass.getClassLoader();
      for (Element listItemElement : listElement.getChildren(ATTR_ITEM)) {
        if (!ATTR_ITEM.equals(listItemElement.getName())) {
          throw new InvalidDataException(
                  "Unable to read list item. Unknown element found: " + listItemElement.getName());
        }
        String itemClassString = listItemElement.getAttributeValue(ATTR_CLASS);
        Class itemClass;
        try {
          itemClass = Class.forName(itemClassString, true, classLoader);
        }
        catch (ClassNotFoundException ex) {
          throw new InvalidDataException(
                  "Unable to read list item: unable to load class: " + itemClassString + " \n" + ex.getMessage());
        }

        String listItem = listItemElement.getAttributeValue(ATTR_VALUE);

        LOG.assertTrue(String.class.equals(itemClass));

        add(listItem);
      }
    }
  }

  @Override
  public void writeExternal(Element element) {
    int listSize = size();
    Element listElement = new Element(ATTR_LIST);
    listElement.setAttribute(ATTR_LISTSIZE, Integer.toString(listSize));
    element.addContent(listElement);
    for (int i = 0; i < listSize; i++) {
      String listItem = get(i);
      if (listItem != null) {
        Element itemElement = new Element(ATTR_ITEM);
        itemElement.setAttribute(ATTR_INDEX, Integer.toString(i));
        itemElement.setAttribute(ATTR_CLASS, listItem.getClass().getName());
        itemElement.setAttribute(ATTR_VALUE, DefaultJDOMExternalizer.filterXMLCharacters(listItem));
        listElement.addContent(itemElement);
      }
    }
  }
}
