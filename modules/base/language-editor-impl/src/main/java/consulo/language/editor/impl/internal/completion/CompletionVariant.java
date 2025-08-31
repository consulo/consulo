/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.language.editor.impl.internal.completion;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupItem;
import consulo.language.editor.completion.lookup.TailType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.filter.ElementFilter;
import consulo.util.lang.reflect.ReflectionUtil;
import org.jetbrains.annotations.NonNls;

import java.util.*;

/**
 * @author ik
 */
public class CompletionVariant {
  protected static final TailType DEFAULT_TAIL_TYPE = TailType.SPACE;

  private final Set<Scope> myScopeClasses = new HashSet<>();
  private ElementFilter myPosition;
  private final List<CompletionVariantItem> myCompletionsList = new ArrayList<>();
  private InsertHandler myInsertHandler = null;
  private final Map<Object, Object> myItemProperties = new HashMap<>();

  public CompletionVariant() {
  }

  public CompletionVariant(Class scopeClass, ElementFilter position) {
    includeScopeClass(scopeClass);
    myPosition = position;
  }

  public CompletionVariant(ElementFilter position) {
    myPosition = position;
  }

  boolean isScopeAcceptable(PsiElement scope) {
    return isScopeClassAcceptable(scope.getClass());
  }

  boolean isScopeFinal(PsiElement scope) {
    return isScopeClassFinal(scope.getClass());
  }

  InsertHandler getInsertHandler() {
    return myInsertHandler;
  }

  public void setInsertHandler(InsertHandler handler) {
    myInsertHandler = handler;
  }

  Map<Object, Object> getItemProperties() {
    return myItemProperties;
  }

  private boolean isScopeClassFinal(Class scopeClass) {
    for (Object myScopeClass : myScopeClasses) {
      Scope scope = (Scope)myScopeClass;
      if (ReflectionUtil.isAssignable(scope.myClass, scopeClass) && scope.myIsFinalScope) {
        return true;
      }
    }
    return false;
  }

  private boolean isScopeClassAcceptable(Class scopeClass) {
    boolean ret = false;

    for (Object myScopeClass : myScopeClasses) {
      Class aClass = ((Scope)myScopeClass).myClass;
      if (ReflectionUtil.isAssignable(aClass, scopeClass)) {
        ret = true;
        break;
      }
    }

    return ret;
  }

  public void includeScopeClass(Class<?> aClass) {
    myScopeClasses.add(new Scope(aClass, false));
  }

  public void includeScopeClass(Class<?> aClass, boolean isFinalScope) {
    myScopeClasses.add(new Scope(aClass, isFinalScope));
  }

  public void addCompletionFilter(ElementFilter filter, TailType tailType) {
    addCompletion(filter, tailType);
  }

  public void addCompletionFilter(ElementFilter filter) {
    addCompletionFilter(filter, TailType.NONE);
  }

  public void addCompletion(@NonNls String keyword) {
    addCompletion(keyword, DEFAULT_TAIL_TYPE);
  }

  public void addCompletion(@NonNls String keyword, TailType tailType) {
    addCompletion((Object)keyword, tailType);
  }

  private void addCompletion(Object completion, TailType tail) {
    myCompletionsList.add(new CompletionVariantItem(completion, tail));
  }

  boolean isVariantApplicable(PsiElement position, PsiElement scope) {
    return isScopeAcceptable(scope) && myPosition.isAcceptable(position, scope);
  }

  void addReferenceCompletions(PsiReference reference, PsiElement position, Set<LookupElement> set, PsiFile file, CompletionData completionData) {
    for (CompletionVariantItem ce : myCompletionsList) {
      if (ce.myCompletion instanceof ElementFilter) {
        ElementFilter filter = (ElementFilter)ce.myCompletion;
        completionData.completeReference(reference, position, set, ce.myTailType, filter, this);
      }
    }
  }

  void addKeywords(Set<LookupElement> set, CompletionData completionData) {
    for (CompletionVariantItem ce : myCompletionsList) {
      completionData.addKeywords(set, this, ce.myCompletion, ce.myTailType);
    }
  }

  boolean hasReferenceFilter() {
    for (CompletionVariantItem item : myCompletionsList) {
      if (item.myCompletion instanceof ElementFilter) {
        return true;
      }
    }
    return false;
  }

  public boolean hasKeywordCompletions() {
    for (CompletionVariantItem item : myCompletionsList) {
      if (!(item.myCompletion instanceof ElementFilter)) {
        return true;
      }
    }
    return false;
  }


  private static class Scope {
    Class myClass;
    boolean myIsFinalScope;

    Scope(Class aClass, boolean isFinalScope) {
      myClass = aClass;
      myIsFinalScope = isFinalScope;
    }
  }

  private static class CompletionVariantItem {
    public Object myCompletion;
    public TailType myTailType;

    public CompletionVariantItem(Object completion, TailType tailtype) {
      myCompletion = completion;
      myTailType = tailtype;
    }

    public String toString() {
      return myCompletion.toString();
    }
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return "completion variant at " + myPosition.toString() + " completions: " + myCompletionsList;
  }

  public void setCaseInsensitive(boolean caseInsensitive) {
    myItemProperties.put(LookupItem.CASE_INSENSITIVE, caseInsensitive);
  }

}
