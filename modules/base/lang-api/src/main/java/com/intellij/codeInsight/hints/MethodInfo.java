/*
 * Copyright 2013-2017 consulo.io
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.codeInsight.hints;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.Objects;

/**
 * from kotlin
 */
public class MethodInfo {
  private String myFullyQualifiedName;
  private List<String> myParamNames;

  public MethodInfo(@Nonnull String fullyQualifiedName, @Nonnull List<String> paramNames) {
    myFullyQualifiedName = fullyQualifiedName;
    myParamNames = paramNames;
  }

  @Nonnull
  public String getFullyQualifiedName() {
    return myFullyQualifiedName;
  }

  @Nonnull
  public List<String> getParamNames() {
    return myParamNames;
  }

  public String getMethodName() {
    int start = myFullyQualifiedName.lastIndexOf('.') + 1;
    return myFullyQualifiedName.substring(start);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MethodInfo)) return false;
    MethodInfo that = (MethodInfo)o;
    return Objects.equals(myFullyQualifiedName, that.myFullyQualifiedName) && Objects.equals(myParamNames, that.myParamNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myFullyQualifiedName, myParamNames);
  }
}
