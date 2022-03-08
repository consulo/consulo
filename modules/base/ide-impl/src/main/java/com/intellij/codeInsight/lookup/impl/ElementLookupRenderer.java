/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.lookup.LookupItem;
import consulo.language.editor.completion.LookupElementPresentation;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.completion.LookupElement;

/**
 * @author yole
 * @deprecated use {@link LookupElement#renderElement(LookupElementPresentation)}
 */
@Deprecated
public interface ElementLookupRenderer<T> {
  ExtensionPointName<ElementLookupRenderer> EP_NAME = ExtensionPointName.create("consulo.elementLookupRenderer");

  boolean handlesItem(Object element);
  void renderElement(final LookupItem item, T element, LookupElementPresentation presentation);

}
