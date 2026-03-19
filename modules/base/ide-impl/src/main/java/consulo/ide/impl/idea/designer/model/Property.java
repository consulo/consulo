/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.designer.model;

import consulo.ide.impl.idea.designer.propertyTable.PropertyEditor;
import consulo.ide.impl.idea.designer.propertyTable.PropertyRenderer;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Alexander Lobas
 */
public abstract class Property<T extends PropertiesContainer> {
  private final Property myParent;
  private final String myName;
  private boolean myImportant;
  private boolean myExpert;
  private boolean myDeprecated;

  public Property(@Nullable Property parent, String name) {
    myParent = parent;
    myName = name;
  }

  public @Nullable Property<T> createForNewPresentation() {
    return createForNewPresentation(myParent, myName);
  }

  public @Nullable Property<T> createForNewPresentation(@Nullable Property parent, String name) {
    return null;
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  //
  // Hierarchy
  //
  //////////////////////////////////////////////////////////////////////////////////////////

  public @Nullable String getGroup() {
    return null;
  }

  public final @Nullable Property getParent() {
    return myParent;
  }

  
  public List<? extends Property<T>> getChildren(@Nullable T container) {
    return Collections.emptyList();
  }

  
  public String getPath() {
    return myParent == null ? myName : myParent.getPath() + "/" + myName;
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  //
  // Value
  //
  //////////////////////////////////////////////////////////////////////////////////////////

  public @Nullable Object getValue(T container) throws Exception {
    return null;
  }

  public void setValue(T container, @Nullable Object value) throws Exception {
  }

  public boolean showAsDefault(T container) throws Exception {
    return isDefaultRecursively(container);
  }

  public final boolean isDefaultRecursively(T container) throws Exception {
    if (!isDefaultValue(container)) return false;
    for (Property<T> child : getChildren(container)) {
      if (!child.isDefaultRecursively(container)) return false;
    }
    return true;
  }

  public boolean isDefaultValue(T container) throws Exception {
    return true;
  }

  public void setDefaultValue(T container) throws Exception {
  }

  public boolean availableFor(List<PropertiesContainer> components) {
    return true;
  }

  public boolean needRefreshPropertyList(T container, @Nullable Object oldValue, @Nullable Object newValue) throws Exception {
    return false;
  }

  public boolean needRefreshPropertyList() {
    return false;
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  //
  // Presentation
  //
  //////////////////////////////////////////////////////////////////////////////////////////

  
  public final String getName() {
    return myName;
  }

  public @Nullable String getTooltip() {
    return null;
  }

  public boolean isImportant() {
    return myImportant;
  }

  public void setImportant(boolean important) {
    myImportant = important;
  }

  public boolean isExpert() {
    return myExpert;
  }

  public void setExpert(boolean expert) {
    myExpert = expert;
  }

  public boolean isDeprecated() {
    return myDeprecated;
  }

  public void setDeprecated(boolean deprecated) {
    myDeprecated = deprecated;
  }

  
  public abstract PropertyRenderer getRenderer();

  public abstract @Nullable PropertyEditor getEditor();

  //////////////////////////////////////////////////////////////////////////////////////////
  //
  // Javadoc
  //
  //////////////////////////////////////////////////////////////////////////////////////////

  public @Nullable PsiElement getJavadocElement() {
    return null;
  }

  public @Nullable String getJavadocText() {
    return null;
  }
}