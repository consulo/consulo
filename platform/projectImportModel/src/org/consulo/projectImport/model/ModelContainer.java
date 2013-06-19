/*
 * Copyright 2013 Consulo.org
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
package org.consulo.projectImport.model;

import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 17:16/19.06.13
 */
public abstract class ModelContainer {
  protected List<Object> myChildren = new ArrayList<Object>();

  public void addChild(@NotNull Object o) {
    myChildren.add(o);
  }

  @NotNull
  public <T> T findChildOrCreate(Class<T> clazz) {
    T child = findChild(clazz);
    if(child == null) {
      final Constructor<T> defaultConstructor = ReflectionUtil.getDefaultConstructor(clazz);
      myChildren.add(child = ReflectionUtil.createInstance(defaultConstructor));
    }
    return child;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T findChild(Class<T> clazz) {
    for(Object o : myChildren) {
      if (clazz.isInstance(o)) {
        return (T)o;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @NotNull
  public <T> List<T> findChildren(Class<T> clazz) {
    List<T> list = new ArrayList<T>(5);
    for(Object o : myChildren) {
      if (clazz.isInstance(o)) {
        list.add((T)o);
      }
    }
    return list.isEmpty() ? Collections.<T>emptyList() : list;
  }
}
