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


import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.ui.SimpleColoredText;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public interface UmlElementManager<T> extends UmlProviderHolder<T> {
  SimpleTextAttributes DEFAULT_TEXT_ATTR = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, Color.BLACK);
  SimpleTextAttributes DEFAULT_TITLE_ATTR = new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, Color.BLACK);
  @Nullable T findInDataContext(DataContext context);
  boolean isAcceptableAsNode(Object element);
  Object[] getNodeElements(T parent);
  Icon getNodeElementIcon(Object element);
  boolean canCollapse(T element);
  boolean isContainerFor(T container, T element);
  @Nullable String getElementTitle(T element);
  @Nullable SimpleColoredText getPresentableName(Object element);
  @Nullable SimpleColoredText getPresentableType(Object element);
  String getElementDescription(T element);
}
