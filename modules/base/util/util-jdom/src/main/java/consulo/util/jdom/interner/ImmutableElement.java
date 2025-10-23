// Copyright 2000-2017 JetBrains s.r.o.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package consulo.util.jdom.interner;

import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import org.jdom.*;
import org.jdom.filter.ElementFilter;
import org.jdom.filter.Filter;

import jakarta.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

class ImmutableElement extends Element {
  private static final List<Attribute> EMPTY_LIST = new ImmutableSameTypeAttributeList(new String[0], AttributeType.UNDECLARED, Namespace.NO_NAMESPACE);
  private final Content[] myContent;
  private static final Content[] EMPTY_CONTENT = new Content[0];
  private final List<Attribute> myAttributes;

  ImmutableElement(@Nonnull Element origin, @Nonnull JDOMInterner interner) {
    // FIXME [VISTALL] we can't do that 'content = null;'

    name = interner.internString(origin.getName());

    List<Attribute> originAttributes = origin.getAttributes();
    String[] nameValues = new String[originAttributes.size() * 2];
    AttributeType type = AttributeType.UNDECLARED;
    Namespace namespace = null;
    for (int i = 0; i < originAttributes.size(); i++) {
      Attribute origAttribute = originAttributes.get(i);
      if (type == AttributeType.UNDECLARED) {
        type = origAttribute.getAttributeType();
        namespace = origAttribute.getNamespace();
      }
      else if (type != origAttribute.getAttributeType() || !origAttribute.getNamespace().equals(namespace)) {
        type = AttributeType.UNDECLARED;
        break; // no single type/namespace, fallback to ImmutableAttrList
      }
      String name = interner.internString(origAttribute.getName());
      String value = interner.internString(origAttribute.getValue());
      nameValues[i * 2] = name;
      nameValues[i * 2 + 1] = value;
    }
    List<Attribute> newAttributes;
    if (originAttributes.isEmpty()) {
      newAttributes = EMPTY_LIST;
    }
    else if (type == AttributeType.UNDECLARED) {
      newAttributes = Collections.unmodifiableList(ContainerUtil.map(originAttributes,
                                                                     attribute -> new ImmutableAttribute(interner.internString(attribute.getName()), interner.internString(attribute.getValue()),
                                                                                                         attribute.getAttributeType(), attribute.getNamespace())));
    }
    else {
      newAttributes = new ImmutableSameTypeAttributeList(nameValues, type, namespace);
    }
    myAttributes = newAttributes;

    List<Content> origContent = origin.getContent();
    List<Content> newContent = new ArrayList<Content>(origContent.size());
    for (Content o : origContent) {
      if (o instanceof Element) {
        Element newElement = interner.intern((Element)o);
        newContent.add(newElement);
      }
      else if (o instanceof Text) {
        Text newText = interner.internText((Text)o);
        newContent.add(newText);
      }
      else if (o instanceof Comment) {
        // ignore
      }
      else {
        throw new RuntimeException(o.toString());
      }
    }

    myContent = newContent.isEmpty() ? EMPTY_CONTENT : newContent.toArray(EMPTY_CONTENT); // ContentList is final, can't subclass

    this.namespace = origin.getNamespace();
    for (Namespace addNs : origin.getAdditionalNamespaces()) {
      super.addNamespaceDeclaration(addNs);
    }
  }

  @Override
  public int getContentSize() {
    return myContent.length;
  }

  @Nonnull
  @Override
  public List<Content> getContent() {
    return Arrays.asList(myContent);
  }

  @Override
  public <T extends Content> List<T> getContent(Filter<T> filter) {
    return (List<T>)ContainerUtil.filter(myContent, content -> filter.matches(content));
  }

  @Override
  public Content getContent(int index) {
    return myContent[index];
  }

  @Override
  public org.jdom.util.IteratorIterable<Content> getDescendants() {
    throw immutableError(this);
  }

  @Override
  public <T extends Content> org.jdom.util.IteratorIterable<T> getDescendants(Filter<T> filter) {
    throw immutableError(this);
  }

  @Nonnull
  @Override
  public List<Element> getChildren() {
    return getContent(new ElementFilter());
  }

  @Nonnull
  @Override
  public List<Element> getChildren(String name, Namespace ns) {
    return getContent(new ElementFilter(name, ns));
  }

  @Override
  public Element getChild(String name, Namespace ns) {
    List<Element> children = getChildren(name, ns);
    return children.isEmpty() ? null : children.get(0);
  }

  @Override
  public String getText() {
    if (myContent.length == 0) {
      return "";
    }

    // If we hold only a Text or CDATA, return it directly
    if (myContent.length == 1) {
      Object obj = myContent[0];
      if (obj instanceof Text) {
        return ((Text)obj).getText();
      }
      else {
        return "";
      }
    }

    // Else build String up
    StringBuilder textContent = new StringBuilder();
    boolean hasText = false;

    for (Content content : myContent) {
      if (content instanceof Text) {
        textContent.append(((Text)content).getText());
        hasText = true;
      }
    }

    return hasText ? textContent.toString() : "";
  }

  @Override
  public int indexOf(Content child) {
    return ArrayUtil.indexOf(myContent, child);
  }

  @Override
  public Namespace getNamespace(String prefix) {
    Namespace ns = super.getNamespace(prefix);
    if (ns == null) {
      for (Attribute a : myAttributes) {
        if (prefix.equals(a.getNamespacePrefix())) {
          return a.getNamespace();
        }
      }
    }
    return ns;
  }

  @Override
  public List<Attribute> getAttributes() {
    return myAttributes;
  }

  @Override
  public Attribute getAttribute(String name, Namespace ns) {
    if (myAttributes instanceof ImmutableSameTypeAttributeList) {
      return ((ImmutableSameTypeAttributeList)myAttributes).get(name, ns);
    }
    String uri = namespace.getURI();
    for (int i = 0; i < myAttributes.size(); i++) {
      Attribute a = myAttributes.get(i);
      String oldURI = a.getNamespaceURI();
      String oldName = a.getName();
      if (oldURI.equals(uri) && oldName.equals(name)) {
        return a;
      }
    }
    return null;
  }

  @Override
  public String getAttributeValue(String name, Namespace ns, String def) {
    if (myAttributes instanceof ImmutableSameTypeAttributeList) {
      return ((ImmutableSameTypeAttributeList)myAttributes).getValue(name, ns, def);
    }
    Attribute attribute = getAttribute(name, ns);
    return attribute == null ? def : attribute.getValue();
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public Element clone() {
    Element element = new Element(name, namespace);

    // Cloning attributes
    List<Attribute> attributes = getAttributes();
    if (attributes != null) {
      element.setAttributes(attributes.stream().map(Attribute::clone).collect(Collectors.toList()));
    }

    // Cloning additional namespaces
    List<Namespace> additionalNamespaces = getAdditionalNamespaces();
    for (Namespace additionalNamespace : additionalNamespaces) {
      element.addNamespaceDeclaration(additionalNamespace);
    }

    // Cloning content
    List<Content> content = getContent();
    for (Content c : content) {
      element.addContent(c.clone());
    }

    return element;
  }

  @Override
  public Element getParent() {
    throw immutableError(this);
  }

  public boolean attributesEqual(Element element) {
    List<Attribute> attrs = element.getAttributes();
    if (myAttributes instanceof ImmutableSameTypeAttributeList) {
      return myAttributes.equals(attrs);
    }

    if (myAttributes.size() != attrs.size()) return false;
    for (int i = 0; i < myAttributes.size(); i++) {
      Attribute attribute = myAttributes.get(i);
      Attribute oAttr = attrs.get(i);
      if (!attributesEqual(attribute, oAttr)) return false;
    }
    return true;
  }

  public static boolean attributesEqual(Attribute a1, Attribute a2) {
    return a1.getName().equals(a2.getName()) && Comparing.equal(a1.getValue(), a2.getValue()) && a1.getAttributeType() == a2.getAttributeType() && a1.getNamespace().equals(a2.getNamespace());
  }

  @Nonnull
  static UnsupportedOperationException immutableError(Object element) {
    return new UnsupportedOperationException("Can't change immutable element: " + element.getClass() + ". To obtain mutable Element call .clone()");
  }

  //////////////////////////////////////////////////////////////////////
  @Override
  public Element detach() {
    throw immutableError(this);
  }

  @Override
  public Element setName(String name) {
    throw immutableError(this);
  }

  @Override
  public Element setNamespace(Namespace namespace) {
    throw immutableError(this);
  }

  @Override
  public boolean addNamespaceDeclaration(Namespace additionalNamespace) {
    throw immutableError(this);
  }

  @Override
  public void removeNamespaceDeclaration(Namespace additionalNamespace) {
    throw immutableError(this);
  }

  @Override
  public Element setText(String text) {
    throw immutableError(this);
  }

  @Override
  public List<Content> removeContent() {
    throw immutableError(this);
  }

  @Override
  public <T extends Content> List<T> removeContent(Filter<T> filter) {
    throw immutableError(this);
  }

  @Override
  public Element setContent(Collection<? extends Content> newContent) {
    throw immutableError(this);
  }

  @Override
  public Element setContent(int index, Content child) {
    throw immutableError(this);
  }

  @Override
  public Parent setContent(int index, Collection<? extends Content> newContent) {
    throw immutableError(this);
  }

  @Override
  public Element addContent(String str) {
    throw immutableError(this);
  }

  @Override
  public Element addContent(Content child) {
    throw immutableError(this);
  }

  @Override
  public Element addContent(Collection<? extends Content> newContent) {
    throw immutableError(this);
  }

  @Override
  public Element addContent(int index, Content child) {
    throw immutableError(this);
  }

  @Override
  public Element addContent(int index, Collection<? extends Content> newContent) {
    throw immutableError(this);
  }

  @Override
  public boolean removeContent(Content child) {
    throw immutableError(this);
  }

  @Override
  public Content removeContent(int index) {
    throw immutableError(this);
  }

  @Override
  public Element setContent(Content child) {
    throw immutableError(this);
  }

  @Override
  public Element setAttributes(Collection<? extends Attribute> newAttributes) {
    throw immutableError(this);
  }

  @Override
  public Element setAttribute(@Nonnull String name, @Nonnull String value) {
    throw immutableError(this);
  }

  @Override
  public Element setAttribute(@Nonnull String name, @Nonnull String value, Namespace ns) {
    throw immutableError(this);
  }

  @Override
  public Element setAttribute(@Nonnull Attribute attribute) {
    throw immutableError(this);
  }

  @Override
  public boolean removeAttribute(String name) {
    throw immutableError(this);
  }

  @Override
  public boolean removeAttribute(String name, Namespace ns) {
    throw immutableError(this);
  }

  @Override
  public boolean removeAttribute(Attribute attribute) {
    throw immutableError(this);
  }

  @Override
  public boolean removeChild(String name) {
    throw immutableError(this);
  }

  @Override
  public boolean removeChild(String name, Namespace ns) {
    throw immutableError(this);
  }

  @Override
  public boolean removeChildren(String name) {
    throw immutableError(this);
  }

  @Override
  public boolean removeChildren(String name, Namespace ns) {
    throw immutableError(this);
  }

  @Override
  protected Content setParent(Parent parent) {
    throw immutableError(this);
    //return null; // to be able to add this element to other's content
  }
}
