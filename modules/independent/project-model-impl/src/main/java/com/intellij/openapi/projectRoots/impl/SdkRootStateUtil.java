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

package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.projectRoots.ex.SdkRoot;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

/**
 * @author mike
 */
public class SdkRootStateUtil {
  @NonNls
  public static final String SIMPLE_ROOT = "simple";
  @NonNls
  public static final String COMPOSITE_ROOT = "composite";
  @NonNls
  private static final String ATTRIBUTE_TYPE = "type";
  @NonNls
  public static final String ELEMENT_ROOT = "root";

  private SdkRootStateUtil() {
  }

  @Nonnull
  static SdkRoot readRoot(Element element) {
    final String type = element.getAttributeValue(ATTRIBUTE_TYPE);

    if (type.equals(SIMPLE_ROOT)) {
      final SimpleSdkRoot root = new SimpleSdkRoot();
      root.readExternal(element);
      return root;
    }
    if (type.equals(COMPOSITE_ROOT)) {
      final CompositeSdkRoot root = new CompositeSdkRoot();
      root.readExternal(element);
      return root;
    }
    throw new IllegalArgumentException("Wrong type: " + type);
  }

  @Nonnull
  static Element writeRoot(SdkRoot sdkRoot) {
    Element element = new Element(ELEMENT_ROOT);
    if (sdkRoot instanceof SimpleSdkRoot) {
      element.setAttribute(ATTRIBUTE_TYPE, SIMPLE_ROOT);
      ((SimpleSdkRoot)sdkRoot).writeExternal(element);
    }
    else if (sdkRoot instanceof CompositeSdkRoot) {
      element.setAttribute(ATTRIBUTE_TYPE, COMPOSITE_ROOT);
      ((CompositeSdkRoot)sdkRoot).writeExternal(element);
    }
    else {
      throw new IllegalArgumentException("Wrong root: " + sdkRoot);
    }

    return element;
  }
}
