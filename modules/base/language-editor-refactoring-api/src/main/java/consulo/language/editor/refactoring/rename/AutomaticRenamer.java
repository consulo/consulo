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

package consulo.language.editor.refactoring.rename;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.logging.Logger;
import consulo.usage.UsageInfo;
import consulo.util.collection.ContainerUtil;
import org.jetbrains.annotations.NonNls;

import java.util.*;

/**
 * @author dsl
 */
public abstract class AutomaticRenamer {
  private static final Logger LOG = Logger.getInstance(AutomaticRenamer.class);
  private final LinkedHashMap<PsiNamedElement, String> myRenames = new LinkedHashMap<PsiNamedElement, String>();
  protected final List<PsiNamedElement> myElements;

  protected AutomaticRenamer() {
    myElements = new ArrayList<PsiNamedElement>();
  }

  public boolean hasAnythingToRename() {
    Collection<String> strings = myRenames.values();
    for (String s : strings) {
      if (s != null) return true;
    }
    return false;
  }

  @RequiredReadAction
  public void findUsages(List<UsageInfo> result, boolean searchInStringsAndComments, boolean searchInNonJavaFiles) {
    findUsages(result, searchInStringsAndComments, searchInNonJavaFiles, null);
  }

  @RequiredReadAction
  public void findUsages(List<UsageInfo> result,
                         boolean searchInStringsAndComments,
                         boolean searchInNonJavaFiles,
                         List<UnresolvableCollisionUsageInfo> unresolvedUsages) {
    findUsages(result, searchInStringsAndComments, searchInNonJavaFiles, unresolvedUsages, null);
  }

  @RequiredReadAction
  public void findUsages(List<UsageInfo> result,
                         boolean searchInStringsAndComments,
                         boolean searchInNonJavaFiles,
                         List<UnresolvableCollisionUsageInfo> unresolvedUsages,
                         Map<PsiElement, String> allRenames) {
    for (Iterator<PsiNamedElement> iterator = myElements.iterator(); iterator.hasNext();) {
      PsiNamedElement variable = iterator.next();
      RenameProcessor.assertNonCompileElement(variable);
      boolean success = findUsagesForElement(variable, result, searchInStringsAndComments, searchInNonJavaFiles, unresolvedUsages, allRenames);
      if (!success) {
        iterator.remove();
      }
    }
  }

  @RequiredReadAction
  private boolean findUsagesForElement(PsiNamedElement element,
                                       List<UsageInfo> result,
                                       boolean searchInStringsAndComments,
                                       boolean searchInNonJavaFiles,
                                       List<UnresolvableCollisionUsageInfo> unresolvedUsages,
                                       Map<PsiElement, String> allRenames) {
    String newName = getNewName(element);
    if (newName != null) {

      LinkedHashMap<PsiNamedElement, String> renames = new LinkedHashMap<PsiNamedElement, String>();
      renames.putAll(myRenames);
      if (allRenames != null) {
        for (PsiElement psiElement : allRenames.keySet()) {
          if (psiElement instanceof PsiNamedElement) {
            renames.put((PsiNamedElement)psiElement, allRenames.get(psiElement));
          }
        }
      }
      UsageInfo[] usages = RenameUtil.findUsages(element, newName, searchInStringsAndComments, searchInNonJavaFiles, renames);
      for (UsageInfo usage : usages) {
        if (usage instanceof UnresolvableCollisionUsageInfo) {
          if (unresolvedUsages != null) {
            unresolvedUsages.add((UnresolvableCollisionUsageInfo)usage);
          }
          return false;
        }
      }
      ContainerUtil.addAll(result, usages);
    }
    return true;
  }

  public List<PsiNamedElement> getElements() {
    return Collections.unmodifiableList(myElements);
  }

  public String getNewName(PsiNamedElement namedElement) {
    return myRenames.get(namedElement);
  }

  public Map<PsiNamedElement,String> getRenames() {
    return Collections.unmodifiableMap(myRenames);
  }

  public void setRename(PsiNamedElement element, String replacement) {
    LOG.assertTrue(myRenames.put(element, replacement) != null);
  }

  public void doNotRename(PsiNamedElement element) {
    LOG.assertTrue(myRenames.remove(element) != null);
  }

  protected void suggestAllNames(String oldClassName, String newClassName) {
    NameSuggester suggester = new NameSuggester(oldClassName, newClassName);
    for (int varIndex = myElements.size() - 1; varIndex >= 0; varIndex--) {
      PsiNamedElement element = myElements.get(varIndex);
      String name = element.getName();
      if (!myRenames.containsKey(element)) {
        String newName;
        if (oldClassName.equals(name)) {
          newName = newClassName;
        } else {
          String canonicalName = nameToCanonicalName(name, element);
          String newCanonicalName = suggester.suggestName(canonicalName);
          if (newCanonicalName.length() == 0) {
            LOG.error("oldClassName = " + oldClassName + ", newClassName = " + newClassName + ", name = " + name + ", canonicalName = " +
                      canonicalName + ", newCanonicalName = " + newCanonicalName);
          }
          newName = canonicalNameToName(newCanonicalName, element);
        }
        if (!newName.equals(name)) {
          myRenames.put(element, newName);
        }
        else {
          myRenames.put(element, null);
        }
      }
      if (myRenames.get(element) == null) {
        myElements.remove(varIndex);
      }
    }
  }

  @NonNls
  protected String canonicalNameToName(@NonNls String canonicalName, PsiNamedElement element) {
    return canonicalName;
  }

  protected String nameToCanonicalName(@NonNls String name, PsiNamedElement element) {
    return name;
  }

  public boolean isSelectedByDefault() {
    return false;
  }

  public abstract String getDialogTitle();

  public abstract String getDialogDescription();

  public abstract String entityName();
}
