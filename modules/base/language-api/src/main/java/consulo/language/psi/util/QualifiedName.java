/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.psi.util;

import consulo.index.io.StringRef;
import consulo.language.psi.stub.StubInputStream;
import consulo.language.psi.stub.StubOutputStream;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class QualifiedName implements Comparable<QualifiedName> {
  public static final QualifiedName ROOT = new QualifiedName(0);

  @Nonnull
  private final List<String> myComponents;

  private QualifiedName(int count) {
    myComponents = new ArrayList<>(count);
  }

  public static QualifiedName fromComponents(Collection<String> components) {
    if(components.isEmpty()) {
      return ROOT;
    }
    QualifiedName qName = new QualifiedName(components.size());
    qName.myComponents.addAll(components);
    return qName;
  }

  @Nonnull
  public static QualifiedName fromComponents(String... components) {
    if(components.length == 0) {
      return ROOT;
    }
    QualifiedName result = new QualifiedName(components.length);
    Collections.addAll(result.myComponents, components);
    return result;
  }

  public QualifiedName append(String name) {
    QualifiedName result = new QualifiedName(myComponents.size() + 1);
    result.myComponents.addAll(myComponents);
    result.myComponents.add(name);
    return result;
  }

  public QualifiedName append(QualifiedName qName) {
    QualifiedName result = new QualifiedName(myComponents.size() + qName.getComponentCount());
    result.myComponents.addAll(myComponents);
    result.myComponents.addAll(qName.getComponents());
    return result;
  }

  @Nonnull
  public QualifiedName removeLastComponent() {
    return removeTail(1);
  }

  @Nonnull
  public QualifiedName removeTail(int count) {
    int size = myComponents.size();
    QualifiedName result = new QualifiedName(size);
    result.myComponents.addAll(myComponents);
    for (int i = 0; i < count && !result.myComponents.isEmpty(); i++) {
      result.myComponents.remove(result.myComponents.size() - 1);
    }
    return result;
  }

  @Nonnull
  public QualifiedName removeHead(int count) {
    int size = myComponents.size();
    QualifiedName result = new QualifiedName(size);
    result.myComponents.addAll(myComponents);
    for (int i = 0; i < count && !result.myComponents.isEmpty(); i++) {
      result.myComponents.remove(0);
    }
    return result;
  }

  @Nonnull
  public List<String> getComponents() {
    return myComponents;
  }

  public int getComponentCount() {
    return myComponents.size();
  }

  public boolean matches(String... components) {
    if (myComponents.size() != components.length) {
      return false;
    }
    for (int i = 0; i < myComponents.size(); i++) {
      if (!myComponents.get(i).equals(components[i])) {
        return false;
      }
    }
    return true;
  }

  public boolean matchesPrefix(QualifiedName prefix) {
    if (getComponentCount() < prefix.getComponentCount()) {
      return false;
    }
    for (int i = 0; i < prefix.getComponentCount(); i++) {
      String component = getComponents().get(i);
      if (component == null || !component.equals(prefix.getComponents().get(i))) {
        return false;
      }
    }
    return true;
  }

  public boolean endsWith(@Nonnull String suffix) {
    return suffix.equals(getLastComponent());
  }

  public static void serialize(@Nullable QualifiedName qName, StubOutputStream dataStream) throws IOException {
    if (qName == null) {
      dataStream.writeVarInt(0);
    }
    else {
      dataStream.writeVarInt(qName.getComponentCount());
      for (String s : qName.myComponents) {
        dataStream.writeName(s);
      }
    }
  }

  @Nullable
  public static QualifiedName deserialize(StubInputStream dataStream) throws IOException {
    QualifiedName qName;
    int size = dataStream.readVarInt();
    if (size == 0) {
      qName = null;
    }
    else {
      qName = new QualifiedName(size);
      for (int i = 0; i < size; i++) {
        StringRef name = dataStream.readName();
        qName.myComponents.add(name == null ? null : name.getString());
      }
    }
    return qName;
  }

  @Nullable
  public String getFirstComponent() {
    if (myComponents.isEmpty()) {
      return null;
    }
    return myComponents.get(0);
  }

  @Nullable
  public String getLastComponent() {
    if (myComponents.isEmpty()) {
      return null;
    }
    return myComponents.get(myComponents.size() - 1);
  }

  @Nullable
  public QualifiedName getParent() {
    if (myComponents.isEmpty()) {
      return null;
    }
    return fromComponents(myComponents.subList(0, myComponents.size() - 1));
  }

  @Override
  public String toString() {
    return join(".");
  }

  public String join(String separator) {
    return StringUtil.join(myComponents, separator);
  }

  @Nonnull
  public static QualifiedName fromDottedString(@Nonnull String refName) {
    if(StringUtil.isEmpty(refName)) {
      return ROOT;
    }
    return fromComponents(refName.split("\\."));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    QualifiedName that = (QualifiedName)o;
    return myComponents.equals(that.myComponents);
  }

  @Override
  public int hashCode() {
    return myComponents.hashCode();
  }

  public QualifiedName subQualifiedName(int fromIndex, int toIndex) {
    return fromComponents(myComponents.subList(fromIndex, toIndex));
  }

  @Override
  public int compareTo(@Nonnull QualifiedName other) {
    return toString().compareTo(other.toString());
  }
}
