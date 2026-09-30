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

import java.util.Comparator;

/**
 * @author Konstantin Bulenkov
 */
public class VisibilityLevel {
  public static final VisibilityLevel[] EMPTY_ARRAY = {};
  public static final Comparator<VisibilityLevel> DUMMY_COMPARATOR = new Comparator<VisibilityLevel>() {
    public int compare(VisibilityLevel o1, VisibilityLevel o2) {
      return 0;
    }
  };

  private final String name;
  private final String displayName;

  public VisibilityLevel(String name) {
    this(name, name);
  }
  
  public VisibilityLevel(String name, String displayName) {
    this.name = name;
    this.displayName = displayName;
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }
}
