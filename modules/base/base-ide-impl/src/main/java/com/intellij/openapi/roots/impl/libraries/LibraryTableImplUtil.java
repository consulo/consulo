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

package com.intellij.openapi.roots.impl.libraries;

import consulo.roots.impl.ModuleRootLayerImpl;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.util.InvalidDataException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nullable;

import java.util.List;

/**
 *  @author dsl
 */
public class LibraryTableImplUtil {
  @NonNls public static final String MODULE_LEVEL = "module";

  private LibraryTableImplUtil() {
  }

  public static Library loadLibrary(Element rootElement, ModuleRootLayerImpl rootModel) throws InvalidDataException {
    final List<Element> children = rootElement.getChildren(LibraryImpl.ELEMENT);
    if (children.size() != 1) throw new InvalidDataException();
    return new LibraryImpl(null, children.get(0), rootModel);
  }

  public static Library createModuleLevelLibrary(@Nullable String name,
                                                 final PersistentLibraryKind kind,
                                                 ModuleRootLayerImpl rootModel) {
    return new LibraryImpl(name, kind, null, rootModel);
  }
}
