// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.navigation.PsiElementNavigationItem;
import com.intellij.openapi.util.ClassConditionKey;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.SmartPsiElementPointer;
import consulo.util.dataholder.UserDataHolderBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * A typical way to create lookup element is to use {@link LookupElementBuilder}.
 * Another way is to subclass it. Use the latter way only if you need it to implement some additional interface, to modify equals/hashCode
 * or other advanced logic.
 *
 * @author peter
 * @see com.intellij.codeInsight.completion.PrioritizedLookupElement
 */
public abstract class LookupElement extends UserDataHolderBase {
  public static final LookupElement[] EMPTY_ARRAY = new LookupElement[0];

  @Nonnull
  public abstract String getLookupString();

  public Set<String> getAllLookupStrings() {
    return Collections.singleton(getLookupString());
  }

  @Nonnull
  public Object getObject() {
    return this;
  }

  /**
   * @return a PSI element associated with this lookup element. It's used for navigation, showing quick documentation and sorting by proximity to the current location.
   * The default implementation tries to extract PSI element from {@link #getObject()} result.
   */
  @Nullable
  public PsiElement getPsiElement() {
    Object o = getObject();
    if (o instanceof PsiElement) {
      return (PsiElement)o;
    }
    if (o instanceof ResolveResult) {
      return ((ResolveResult)o).getElement();
    }
    if (o instanceof PsiElementNavigationItem) {
      return ((PsiElementNavigationItem)o).getTargetElement();
    }
    if (o instanceof SmartPsiElementPointer) {
      return ((SmartPsiElementPointer)o).getElement();
    }
    return null;
  }

  public boolean isValid() {
    final Object object = getObject();
    if (object instanceof PsiElement) {
      return ((PsiElement)object).isValid();
    }
    return true;
  }

  public void handleInsert(@Nonnull InsertionContext context) {
  }

  /**
   * @return whether {@link #handleInsert} expects all documents to be committed at the moment of its invocation.
   * The default is {@code true}, overriders can change that, for example if automatic commit is too slow.
   */
  public boolean requiresCommittedDocuments() {
    return true;
  }

  public AutoCompletionPolicy getAutoCompletionPolicy() {
    return AutoCompletionPolicy.SETTINGS_DEPENDENT;
  }

  @Override
  public String toString() {
    return getLookupString();
  }

  public void renderElement(LookupElementPresentation presentation) {
    presentation.setItemText(getLookupString());
  }

  /**
   * Prefer to use {@link #as(Class)}
   */
  @Nullable
  public <T> T as(ClassConditionKey<T> conditionKey) {
    //noinspection unchecked
    return conditionKey.isInstance(this) ? (T)this : null;
  }

  /**
   * @return a renderer (if any) that performs potentially expensive computations on this lookup element.
   * It's called on a background thread, not blocking this element from being shown to the user.
   * It may return this lookup element's presentation appended with more details than {@link #renderElement} has given.
   * If the {@link Lookup} is already shown, it will be repainted/resized to accommodate the changes.
   */
  @Nullable
  public LookupElementRenderer<? extends LookupElement> getExpensiveRenderer() {
    return null;
  }

  /**
   * Return the first element of the given class in a {@link LookupElementDecorator} wrapper chain.
   * If this object is not a decorator, return it if it's instance of the given class, otherwise null.
   */
  @Nullable
  public <T> T as(Class<T> clazz) {
    //noinspection unchecked
    return clazz.isInstance(this) ? (T)this : null;
  }

  public boolean isCaseSensitive() {
    return true;
  }

  /**
   * Invoked when the completion autopopup contains only the items that exactly match the user-entered prefix to determine
   * whether the popup should be closed to not get in the way when navigating through the code.
   * Should return true if there's some meaningful information in this item's presentation that the user will miss
   * if the autopopup is suddenly closed automatically. Java method parameters are a good example. For simple variables,
   * there's nothing else interesting besides the variable name which is already entered in the editor, so the autopopup may be closed.
   */
  public boolean isWorthShowingInAutoPopup() {
    final LookupElementPresentation presentation = new LookupElementPresentation();
    renderElement(presentation);
    return !presentation.getTailFragments().isEmpty();
  }
}
