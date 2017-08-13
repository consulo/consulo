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

package com.intellij.refactoring.classMembers;

import com.intellij.lang.LanguageDependentMembersRefactoringSupport;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;


public class MemberDependenciesStorage<T extends NavigatablePsiElement, C extends PsiElement> {
  protected final C myClass;
  private final C mySuperClass;
  private final Map<T, Set<T>> myDependencyGraph;

  public MemberDependenciesStorage(C aClass, C superClass) {
    myClass = aClass;
    mySuperClass = superClass;
    myDependencyGraph = new HashMap<T, Set<T>>();
  }

  @Nullable
  protected Set<T> getMemberDependencies(T member) {
    Set<T> result = myDependencyGraph.get(member);
    if (result == null) {
      DependentMembersCollectorBase<T, C> collector = getCollector();
      if (collector != null) {
        collector.collect(member);
        result = collector.getCollection();
      }
      myDependencyGraph.put(member, result);
    }
    return result;
  }

  private DependentMembersCollectorBase<T, C> getCollector() {
    final ClassMembersRefactoringSupport factory = LanguageDependentMembersRefactoringSupport.INSTANCE.forLanguage(myClass.getLanguage());
    return factory != null ? factory.createDependentMembersCollector(myClass, mySuperClass) : null;
  }
}
