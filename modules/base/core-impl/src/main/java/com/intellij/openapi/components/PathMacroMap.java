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
package com.intellij.openapi.components;

import com.intellij.openapi.application.PathMacroFilter;
import consulo.logging.Logger;
import com.intellij.openapi.util.io.FileUtil;
import org.jdom.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Eugene Zhuravlev
 * @since Dec 6, 2004
 */
public abstract class PathMacroMap {
  public static final Logger LOGGER = Logger.getInstance(PathMacroMap.class);

  public abstract String substitute(String text, boolean caseSensitive);

  public final void substitute(@Nonnull Element e, boolean caseSensitive) {
    substitute(e, caseSensitive, false);
  }

  public final void substitute(@Nonnull Element e, boolean caseSensitive, boolean recursively, @Nullable PathMacroFilter filter) {
    for (Content child : e.getContent()) {
      if (child instanceof Element) {
        substitute((Element)child, caseSensitive, recursively, filter);
      }
      else if (child instanceof Text) {
        Text t = (Text)child;
        if (filter == null || !filter.skipPathMacros(t)) {
          String oldText = t.getText();
          String newText = (recursively || (filter != null && filter.recursePathMacros(t)))
                           ? substituteRecursively(oldText, caseSensitive)
                           : substitute(oldText, caseSensitive);
          if (oldText != newText) {
            t.setText(newText);
          }
        }
      }
      else if (!(child instanceof Comment)) {
        LOGGER.error("Wrong content: " + child.getClass());
      }
    }

    for (Attribute attribute : e.getAttributes()) {
      if (filter == null || !filter.skipPathMacros(attribute)) {
        String oldValue = attribute.getValue();
        String newValue = (recursively || (filter != null && filter.recursePathMacros(attribute)))
                          ? substituteRecursively(oldValue, caseSensitive)
                          : substitute(oldValue, caseSensitive);
        if (oldValue != newValue) {
          attribute.setValue(newValue);
        }
      }
    }
  }

  public final void substitute(@Nonnull Element e, boolean caseSensitive, final boolean recursively) {
    substitute(e, caseSensitive, recursively, null);
  }

  public String substituteRecursively(String text, boolean caseSensitive) {
    return substitute(text, caseSensitive);
  }

  protected static String quotePath(String path) {
    return FileUtil.toSystemIndependentName(path);
  }

  @Override
  public abstract int hashCode();
}
