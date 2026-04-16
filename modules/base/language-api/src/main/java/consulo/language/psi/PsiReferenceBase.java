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
package consulo.language.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public abstract class PsiReferenceBase<T extends PsiElement> implements PsiReference {

  private static final Logger LOG = Logger.getInstance(PsiReferenceBase.class);

  protected final T myElement;
  private @Nullable TextRange myRange;
  protected boolean mySoft;

  /**
   * @param element PSI element
   * @param range range relatively to the element's start offset
   * @param soft soft
   */
  public PsiReferenceBase(T element, @Nullable TextRange range, boolean soft) {
    myElement = element;
    myRange = range;
    mySoft = soft;
  }

  /**
   * @param element PSI element
   * @param range range relatively to the element's start offset
   */
  public PsiReferenceBase(T element, TextRange range) {
    this(element, range, false);
  }

  /**
   * The range is obtained from {@link ElementManipulators}
   * @param element PSI element
   * @param soft soft
   */
  public PsiReferenceBase(T element, boolean soft) {
    this(element, null, soft);
  }

  /**
   * The range is obtained from {@link ElementManipulators}
   * @param element PSI element
   */
  public PsiReferenceBase(T element) {
    this(element, false);
  }

  public void setRangeInElement(TextRange range) {
    myRange = range;
  }

  public String getValue() {
    String text = myElement.getText();
    TextRange range = getRangeInElement();
    try {
      return range.substring(text);
    }
    catch (StringIndexOutOfBoundsException e) {
      LOG.error("Wrong range in reference " + this + ": " + range + ". Reference text: '" + text + "'", e);
      return text;
    }
  }

  @Override
  public T getElement() {
    return myElement;
  }

  @Override
  public TextRange getRangeInElement() {
    if (myRange == null) {
      myRange = calculateDefaultRangeInElement();
    }
    return myRange;
  }

  protected TextRange calculateDefaultRangeInElement() {
    return getRequiredManipulator().getRangeInElement(myElement);
  }

  @Override
  public String getCanonicalText() {
    return getValue();
  }

  @Override
  @RequiredWriteAction
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    return getRequiredManipulator().handleContentChange(myElement, getRangeInElement(), newElementName);
  }

  @Override
  public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException("Rebind cannot be performed for " + getClass());
  }

  @Override
  @RequiredReadAction
  public boolean isReferenceTo(PsiElement element) {
    return getElement().getRequiredManager().areElementsEquivalent(resolve(), element);
  }

  public static <T extends PsiElement> PsiReferenceBase<T> createSelfReference(T element, PsiElement resolveTo) {
    return new Immediate<T>(element, true, resolveTo);
  }

  public static <T extends PsiElement> PsiReferenceBase<T> createSelfReference(T element, TextRange range, PsiElement resolveTo) {
    return new Immediate<T>(element, range, resolveTo);
  }

  @Nullable ElementManipulator<T> getManipulator() {
    ElementManipulator<T> manipulator = ElementManipulators.getManipulator(myElement);
    if (manipulator == null) {
      LOG.error("Cannot find manipulator for " + myElement + " in " + this + " class " + getClass());
    }
    return manipulator;
  }

  ElementManipulator<T> getRequiredManipulator() {
    ElementManipulator<T> manipulator = ElementManipulators.getManipulator(myElement);
    if (manipulator == null) {
      throw new NullPointerException("Cannot find manipulator for " + myElement + " in " + this + " class " + getClass());
    }
    return manipulator;
  }

  @Override
  public boolean isSoft() {
    return mySoft;
  }

  public static abstract class Poly<T extends PsiElement> extends PsiReferenceBase<T> implements PsiPolyVariantReference {

    public Poly(T psiElement) {
      super(psiElement);
    }

    public Poly(T element, boolean soft) {
      super(element, soft);
    }

    public Poly(T element, TextRange range, boolean soft) {
      super(element, range, soft);
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
      ResolveResult[] results = multiResolve(false);
      for (ResolveResult result : results) {
        if (element.getRequiredManager().areElementsEquivalent(result.getElement(), element)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public @Nullable PsiElement resolve() {
      ResolveResult[] resolveResults = multiResolve(false);
      return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }
  }

  public static class Immediate<T extends PsiElement> extends PsiReferenceBase<T> {
    private final PsiElement myResolveTo;

    public Immediate(T element, TextRange range, boolean soft, PsiElement resolveTo) {
      super(element, range, soft);
      myResolveTo = resolveTo;
    }

    public Immediate(T element, TextRange range, PsiElement resolveTo) {
      super(element, range);
      myResolveTo = resolveTo;
    }

    public Immediate(T element, boolean soft, PsiElement resolveTo) {
      super(element, soft);
      myResolveTo = resolveTo;
    }

    public Immediate(T element, PsiElement resolveTo) {
      super(element);
      myResolveTo = resolveTo;
    }

    //do nothing. the element will be renamed via PsiMetaData (consulo.ide.impl.idea.refactoring.rename.RenameUtil.doRenameGenericNamedElement())
    @Override
    public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
      return getElement();
    }

    @Override
    public @Nullable PsiElement resolve() {
      return myResolveTo;
    }

    @Override
    public Object[] getVariants() {
      return EMPTY_ARRAY;
    }
  }
}
