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
package consulo.language.editor.refactoring.classMember;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author dsl
 * @since 2002-07-08
 */
public class UsesMemberDependencyGraph<T extends NavigatablePsiElement, C extends PsiElement, M extends MemberInfoBase<T>> implements MemberDependencyGraph<T, M> {
  private static final Logger LOG = Logger.getInstance(UsesMemberDependencyGraph.class);
  protected Set<T> mySelectedNormal;
  protected Set<T> mySelectedAbstract;
  protected Set<T> myDependencies = null;
  protected Map<T, Set<T>> myDependenciesToDependentMap = null;
  private final boolean myRecursive;
  private final MemberDependenciesStorage<T, C> myMemberDependenciesStorage;

  public UsesMemberDependencyGraph(C aClass, C superClass, boolean recursive) {
    myRecursive = recursive;
    mySelectedNormal = new HashSet<>();
    mySelectedAbstract = new HashSet<>();
    myMemberDependenciesStorage = new MemberDependenciesStorage<>(aClass, superClass);
  }


  @Override
  public Set<? extends T> getDependent() {
    if (myDependencies == null) {
      myDependencies = new HashSet<>();
      myDependenciesToDependentMap = new HashMap<>();
      buildDeps(null, mySelectedNormal);
    }
    return myDependencies;
  }

  @Override
  public Set<? extends T> getDependenciesOf(T member) {
    Set dependent = getDependent();
    if (!dependent.contains(member)) return null;
    return myDependenciesToDependentMap.get(member);
  }

  public String getElementTooltip(T element) {
    Set<? extends T> dependencies = getDependenciesOf(element);
    if (dependencies == null || dependencies.size() == 0) return null;

    ArrayList<String> strings = new ArrayList<>();
    for (T dep : dependencies) {
      strings.add(dep.getName());
    }

    if (strings.isEmpty()) return null;
    return RefactoringLocalize.usedBy0(StringUtil.join(strings, ", ")).get();
  }


  private void buildDeps(T sourceElement, Set<T> members) {
    if (myRecursive) {
      buildDepsRecursively(sourceElement, members);
    }
    else {
      for (T member : members) {
        Set<T> dependencies = myMemberDependenciesStorage.getMemberDependencies(member);
        if (dependencies != null) {
          for (T dependency : dependencies) {
            addDependency(dependency, member);
          }
        }
      }
    }
  }

  private void buildDepsRecursively(T sourceElement, @Nullable Set<T> members) {
    if (members != null) {
      for (T member : members) {
        if (!myDependencies.contains(member)) {
          addDependency(member, sourceElement);
          if (!mySelectedAbstract.contains(member)) {
            buildDepsRecursively(member, myMemberDependenciesStorage.getMemberDependencies(member));
          }
        }
      }
    }
  }

  private void addDependency(T member, T sourceElement) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(member.toString());
    }
    myDependencies.add(member);
    if (sourceElement != null) {
      Set<T> relations = myDependenciesToDependentMap.get(member);
      if (relations == null) {
        relations = new HashSet<>();
        myDependenciesToDependentMap.put(member, relations);
      }
      relations.add(sourceElement);
    }
  }

  @Override
  public void memberChanged(M memberInfo) {
    ClassMembersRefactoringSupport support = ClassMembersRefactoringSupport.forLanguage(memberInfo.getMember().getLanguage());
    if (support != null && support.isProperMember(memberInfo)) {
      myDependencies = null;
      myDependenciesToDependentMap = null;
      T member = memberInfo.getMember();
      if (!memberInfo.isChecked()) {
        mySelectedNormal.remove(member);
        mySelectedAbstract.remove(member);
      } else {
        if (memberInfo.isToAbstract()) {
          mySelectedNormal.remove(member);
          mySelectedAbstract.add(member);
        } else {
          mySelectedNormal.add(member);
          mySelectedAbstract.remove(member);
        }
      }
    }
  }
}
