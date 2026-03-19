// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.completion.lookup;

import consulo.language.editor.completion.*;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.*;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.Contract;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author peter
 * @see LookupElementDecorator
 * @see PrioritizedLookupElement
 */
public final class LookupElementBuilder extends LookupElement {
  
  private final String myLookupString;
  
  private final Object myObject;
  private final @Nullable SmartPsiElementPointer<?> myPsiElement;
  private final boolean myCaseSensitive;
  private final @Nullable InsertHandler<LookupElement> myInsertHandler;
  private final @Nullable LookupElementRenderer<LookupElement> myRenderer;
  private final @Nullable LookupElementRenderer<LookupElement> myExpensiveRenderer;
  private final @Nullable LookupElementPresentation myHardcodedPresentation;
  
  private final Set<String> myAllLookupStrings;

  private LookupElementBuilder(String lookupString,
                               Object object,
                               @Nullable InsertHandler<LookupElement> insertHandler,
                               @Nullable LookupElementRenderer<LookupElement> renderer,
                               @Nullable LookupElementRenderer<LookupElement> expensiveRenderer,
                               @Nullable LookupElementPresentation hardcodedPresentation,
                               @Nullable SmartPsiElementPointer<?> psiElement,
                               Set<String> allLookupStrings,
                               boolean caseSensitive) {
    myLookupString = lookupString;
    myObject = object;
    myInsertHandler = insertHandler;
    myRenderer = renderer;
    myExpensiveRenderer = expensiveRenderer;
    myHardcodedPresentation = hardcodedPresentation;
    myPsiElement = psiElement;
    myAllLookupStrings = Collections.unmodifiableSet(allLookupStrings);
    myCaseSensitive = caseSensitive;
  }

  private LookupElementBuilder(String lookupString, Object object) {
    this(lookupString, object, null, null, null, null, null, Collections.singleton(lookupString), true);
  }

  
  public static LookupElementBuilder create(String lookupString) {
    return new LookupElementBuilder(lookupString, lookupString);
  }

  
  public static LookupElementBuilder create(Object object) {
    return new LookupElementBuilder(object.toString(), object);
  }

  
  public static LookupElementBuilder createWithSmartPointer(String lookupString, PsiElement element) {
    PsiUtilCore.ensureValid(element);
    return new LookupElementBuilder(lookupString, SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element));
  }

  
  public static LookupElementBuilder create(PsiNamedElement element) {
    PsiUtilCore.ensureValid(element);
    return new LookupElementBuilder(StringUtil.notNullize(element.getName()), element);
  }

  
  public static LookupElementBuilder createWithIcon(PsiNamedElement element) {
    PsiUtilCore.ensureValid(element);
    return create(element).withIcon(IconDescriptorUpdaters.getIcon(element, 0));
  }

  
  public static LookupElementBuilder create(Object lookupObject, String lookupString) {
    if (lookupObject instanceof PsiElement) {
      PsiUtilCore.ensureValid((PsiElement)lookupObject);
    }
    return new LookupElementBuilder(lookupString, lookupObject);
  }

  
  private LookupElementBuilder cloneWithUserData(String lookupString,
                                                 Object object,
                                                 @Nullable InsertHandler<LookupElement> insertHandler,
                                                 @Nullable LookupElementRenderer<LookupElement> renderer,
                                                 @Nullable LookupElementRenderer<LookupElement> expensiveRenderer,
                                                 @Nullable LookupElementPresentation hardcodedPresentation,
                                                 @Nullable SmartPsiElementPointer<?> psiElement,
                                                 Set<String> allLookupStrings,
                                                 boolean caseSensitive) {
    LookupElementBuilder result = new LookupElementBuilder(lookupString, object, insertHandler, renderer, expensiveRenderer, hardcodedPresentation, psiElement, allLookupStrings, caseSensitive);
    copyUserDataTo(result);
    return result;
  }

  /**
   * @deprecated use {@link #withInsertHandler(InsertHandler)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setInsertHandler(@Nullable InsertHandler<LookupElement> insertHandler) {
    return withInsertHandler(insertHandler);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withInsertHandler(@Nullable InsertHandler<LookupElement> insertHandler) {
    return cloneWithUserData(myLookupString, myObject, insertHandler, myRenderer, myExpensiveRenderer, myHardcodedPresentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  /**
   * @deprecated use {@link #withRenderer(LookupElementRenderer)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setRenderer(@Nullable LookupElementRenderer<LookupElement> renderer) {
    return withRenderer(renderer);
  }

  @Contract(pure = true)
  public
  
  LookupElementBuilder withRenderer(@Nullable LookupElementRenderer<LookupElement> renderer) {
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, renderer, myExpensiveRenderer, myHardcodedPresentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  @Contract(pure = true)
  public
  
  LookupElementBuilder withExpensiveRenderer(@Nullable LookupElementRenderer<LookupElement> expensiveRenderer) {
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, myRenderer, expensiveRenderer, myHardcodedPresentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  @Override
  
  public Set<String> getAllLookupStrings() {
    return myAllLookupStrings;
  }

  /**
   * @deprecated use {@link #withIcon(Icon)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setIcon(@Nullable Image icon) {
    return withIcon(icon);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withIcon(@Nullable Image icon) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setIcon(icon);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  
  private LookupElementPresentation copyPresentation() {
    LookupElementPresentation presentation = new LookupElementPresentation();
    if (myHardcodedPresentation != null) {
      presentation.copyFrom(myHardcodedPresentation);
    }
    else {
      presentation.setItemText(myLookupString);
    }
    return presentation;
  }

  /**
   * @deprecated use {@link #withLookupString(String)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder addLookupString(String another) {
    return withLookupString(another);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withLookupString(String another) {
    Set<String> set = new HashSet<>(myAllLookupStrings);
    set.add(another);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, myRenderer, myExpensiveRenderer, myHardcodedPresentation, myPsiElement, Collections.unmodifiableSet(set), myCaseSensitive);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withLookupStrings(Collection<String> another) {
    Set<String> set = new HashSet<>(myAllLookupStrings.size() + another.size());
    set.addAll(myAllLookupStrings);
    set.addAll(another);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, myRenderer, myExpensiveRenderer, myHardcodedPresentation, myPsiElement, Collections.unmodifiableSet(set), myCaseSensitive);
  }

  @Override
  public boolean isCaseSensitive() {
    return myCaseSensitive;
  }

  /**
   * @deprecated use {@link #withCaseSensitivity(boolean)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setCaseSensitive(boolean caseSensitive) {
    return withCaseSensitivity(caseSensitive);
  }

  /**
   * @param caseSensitive if this lookup item should be completed in the same letter case as prefix
   * @return modified builder
   * @see CompletionResultSet#caseInsensitive()
   */
  @Contract(pure = true)
  
  public LookupElementBuilder withCaseSensitivity(boolean caseSensitive) {
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, myRenderer, myExpensiveRenderer, myHardcodedPresentation, myPsiElement, myAllLookupStrings, caseSensitive);
  }

  /**
   * Allows to pass custom PSI that will be returned from {@link #getPsiElement()}.
   */
  @Contract(pure = true)
  public
  
  LookupElementBuilder withPsiElement(@Nullable PsiElement psi) {
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, myRenderer, myExpensiveRenderer, myHardcodedPresentation, psi == null ? null : SmartPointerManager.createPointer(psi),
                             myAllLookupStrings, myCaseSensitive);
  }

  /**
   * @deprecated use {@link #withItemTextForeground(Color)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setItemTextForeground(ColorValue itemTextForeground) {
    return withItemTextForeground(itemTextForeground);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withItemTextForeground(ColorValue itemTextForeground) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setItemTextForeground(itemTextForeground);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  /**
   * @deprecated use {@link #withItemTextUnderlined(boolean)}
   */
  @Deprecated
  @Contract(pure = true)
  public
  
  LookupElementBuilder setItemTextUnderlined(boolean underlined) {
    return withItemTextUnderlined(underlined);
  }

  @Contract(pure = true)
  public
  
  LookupElementBuilder withItemTextUnderlined(boolean underlined) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setItemTextUnderlined(underlined);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  @Contract(pure = true)
  public
  
  LookupElementBuilder withItemTextItalic(boolean italic) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setItemTextItalic(italic);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  /**
   * @deprecated use {@link #withTypeText(String)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setTypeText(@Nullable String typeText) {
    return withTypeText(typeText);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withTypeText(@Nullable String typeText) {
    return withTypeText(typeText, false);
  }

  /**
   * @deprecated use {@link #withTypeText(String, boolean)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setTypeText(@Nullable String typeText, boolean grayed) {
    return withTypeText(typeText, grayed);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withTypeText(@Nullable String typeText, boolean grayed) {
    return withTypeText(typeText, null, grayed);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withTypeText(@Nullable String typeText, @Nullable Image typeIcon, boolean grayed) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setTypeText(typeText, typeIcon);
    presentation.setTypeGrayed(grayed);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  
  public LookupElementBuilder withTypeIconRightAligned(boolean typeIconRightAligned) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setTypeIconRightAligned(typeIconRightAligned);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  /**
   * @deprecated use {@link #withPresentableText(String)}
   */
  @Deprecated
  @Contract(pure = true)
  public
  
  LookupElementBuilder setPresentableText(String presentableText) {
    return withPresentableText(presentableText);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withPresentableText(String presentableText) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setItemText(presentableText);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  /**
   * @deprecated use {@link #bold()}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setBold() {
    return bold();
  }

  @Contract(pure = true)
  
  public LookupElementBuilder bold() {
    return withBoldness(true);
  }

  /**
   * @deprecated use {@link #withBoldness(boolean)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setBold(boolean bold) {
    return withBoldness(bold);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withBoldness(boolean bold) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setItemTextBold(bold);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  /**
   * @deprecated use {@link #strikeout()}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setStrikeout() {
    return strikeout();
  }

  @Contract(pure = true)
  
  public LookupElementBuilder strikeout() {
    return withStrikeoutness(true);
  }

  /**
   * @deprecated use {@link #withStrikeoutness(boolean)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setStrikeout(boolean strikeout) {
    return withStrikeoutness(strikeout);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withStrikeoutness(boolean strikeout) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setStrikeout(strikeout);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  /**
   * @deprecated use {@link #withTailText(String)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setTailText(@Nullable String tailText) {
    return withTailText(tailText);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withTailText(@Nullable String tailText) {
    return withTailText(tailText, false);
  }

  /**
   * @deprecated use {@link #withTailText(String, boolean)}
   */
  @Deprecated
  @Contract(pure = true)
  
  public LookupElementBuilder setTailText(@Nullable String tailText, boolean grayed) {
    return withTailText(tailText, grayed);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder withTailText(@Nullable String tailText, boolean grayed) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.setTailText(tailText, grayed);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  @Contract(pure = true)
  
  public LookupElementBuilder appendTailText(String tailText, boolean grayed) {
    LookupElementPresentation presentation = copyPresentation();
    presentation.appendTailText(tailText, grayed);
    return cloneWithUserData(myLookupString, myObject, myInsertHandler, null, myExpensiveRenderer, presentation, myPsiElement, myAllLookupStrings, myCaseSensitive);
  }

  @Contract(pure = true)
  public LookupElement withAutoCompletionPolicy(AutoCompletionPolicy policy) {
    return policy.applyPolicy(this);
  }

  
  @Override
  public String getLookupString() {
    return myLookupString;
  }

  public @Nullable InsertHandler<LookupElement> getInsertHandler() {
    return myInsertHandler;
  }

  public @Nullable LookupElementRenderer<LookupElement> getRenderer() {
    return myRenderer;
  }

  @Override
  public @Nullable LookupElementRenderer<? extends LookupElement> getExpensiveRenderer() {
    return myExpensiveRenderer;
  }

  
  @Override
  public Object getObject() {
    return myObject;
  }

  @Nullable
  @Override
  public PsiElement getPsiElement() {
    if (myPsiElement != null) return myPsiElement.getElement();
    return super.getPsiElement();
  }

  @Override
  public void handleInsert(InsertionContext context) {
    if (myInsertHandler != null) {
      myInsertHandler.handleInsert(context, this);
    }
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    if (myRenderer != null) {
      myRenderer.renderElement(this, presentation);
    }
    else if (myHardcodedPresentation != null) {
      presentation.copyFrom(myHardcodedPresentation);
    }
    else {
      presentation.setItemText(myLookupString);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LookupElementBuilder that = (LookupElementBuilder)o;

    InsertHandler<LookupElement> insertHandler = that.myInsertHandler;
    if (myInsertHandler != null && insertHandler != null ? !myInsertHandler.getClass().equals(insertHandler.getClass()) : myInsertHandler != insertHandler) return false;
    if (!myLookupString.equals(that.myLookupString)) return false;
    if (!myObject.equals(that.myObject)) return false;

    LookupElementRenderer<LookupElement> renderer = that.myRenderer;
    if (myRenderer != null && renderer != null ? !myRenderer.getClass().equals(renderer.getClass()) : myRenderer != renderer) return false;

    return true;
  }

  @Override
  public String toString() {
    return "LookupElementBuilder: string=" + getLookupString() + "; handler=" + myInsertHandler;
  }

  @Override
  public int hashCode() {
    int result = 0;
    result = 31 * result + (myInsertHandler != null ? myInsertHandler.getClass().hashCode() : 0);
    result = 31 * result + (myLookupString.hashCode());
    result = 31 * result + (myObject.hashCode());
    result = 31 * result + (myRenderer != null ? myRenderer.getClass().hashCode() : 0);
    return result;
  }

}
