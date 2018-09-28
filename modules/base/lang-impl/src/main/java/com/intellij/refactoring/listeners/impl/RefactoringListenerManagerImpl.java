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

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider;
import com.intellij.refactoring.listeners.RefactoringListenerManager;
import com.intellij.refactoring.listeners.impl.impl.RefactoringTransactionImpl;
import com.intellij.util.containers.ContainerUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author dsl
 */
@Singleton
public class RefactoringListenerManagerImpl extends RefactoringListenerManager {
  private final List<RefactoringElementListenerProvider> myListenerProviders = ContainerUtil.createLockFreeCopyOnWriteList();
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

  public RefactoringTransaction startTransaction() {
    List<RefactoringElementListenerProvider> providers = new ArrayList<RefactoringElementListenerProvider>(myListenerProviders);
    Collections.addAll(providers, Extensions.getExtensions(RefactoringElementListenerProvider.EP_NAME, myProject));
    return new RefactoringTransactionImpl(providers);
  }
}
