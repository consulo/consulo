/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.uml;

import com.intellij.util.Icons;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public class UmlCategory {
  public static final UmlCategory[] EMPTY_ARRAY = {};
  private final String name;
  private final Icon icon;

  public UmlCategory(String name, Icon icon) {
    this.name = name;
    this.icon = icon == null ? Icons.ERROR_INTRODUCTION_ICON : icon;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  //generated
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UmlCategory that = (UmlCategory)o;

    return !(name != null ? !name.equals(that.name) : that.name != null);
  }

  //generated
  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  public Icon getIcon() {
    return icon;
  }
}
