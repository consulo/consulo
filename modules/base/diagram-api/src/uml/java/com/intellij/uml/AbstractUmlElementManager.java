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

import com.intellij.openapi.util.Iconable;
import com.intellij.util.Icons;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public abstract class AbstractUmlElementManager<T> implements UmlElementManager<T> {
  protected UmlProvider<T> myProvider;

  public void setUmlProvider(UmlProvider<T> provider) {
    myProvider = provider;
  }

  public UmlProvider<T> getProvider() {
    return myProvider;
  }

  public Icon getNodeElementIcon(Object element) {
    return element instanceof Iconable ? ((Iconable)element).getIcon(0) : Icons.ERROR_INTRODUCTION_ICON;
  }
}
