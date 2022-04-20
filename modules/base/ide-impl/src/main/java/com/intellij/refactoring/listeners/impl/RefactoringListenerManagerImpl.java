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

package com.intellij.refactoring.listeners.impl;

import com.intellij.refactoring.listeners.impl.impl.RefactoringTransactionImpl;
import consulo.language.editor.refactoring.RefactoringTransaction;
import consulo.language.editor.refactoring.event.RefactoringElementListenerProvider;
import consulo.language.editor.refactoring.internal.RefactoringListenerManagerEx;
import consulo.project.Project;
import consulo.util.collection.Lists;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dsl
 */
@Singleton
public class RefactoringListenerManagerImpl extends RefactoringListenerManagerEx {
  private final List<RefactoringElementListenerProvider> myListenerProviders = Lists.newLockFreeCopyOnWriteList();
  private final Project myProject;

  @Inject
  public RefactoringListenerManagerImpl(Project project) {
    myProject = project;
  }

  @Override
  public void addListenerProvider(RefactoringElementListenerProvider provider) {
    myListenerProviders.add(provider);
  }

  @Override
  public void removeListenerProvider(RefactoringElementListenerProvider provider) {
    myListenerProviders.remove(provider);
  }

  @Override
  public RefactoringTransaction startTransaction() {
    List<RefactoringElementListenerProvider> providers = new ArrayList<>(myListenerProviders);
    providers.addAll(RefactoringElementListenerProvider.EP_NAME.getExtensionList(myProject));
    return new RefactoringTransactionImpl(providers);
  }
}
