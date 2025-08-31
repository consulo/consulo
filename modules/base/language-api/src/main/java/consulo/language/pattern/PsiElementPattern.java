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
package consulo.language.pattern;

import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.util.function.BiPredicate;

/**
 * @author peter
 */
public abstract class PsiElementPattern<T extends PsiElement, Self extends PsiElementPattern<T, Self>> extends TreeElementPattern<PsiElement, T, Self> {
  protected PsiElementPattern(Class<T> aClass) {
    super(aClass);
  }

  protected PsiElementPattern(@Nonnull InitialPatternCondition<T> condition) {
    super(condition);
  }

  @Override
  protected PsiElement[] getChildren(@Nonnull PsiElement element) {
    return element.getChildren();
  }

  @Override
  protected PsiElement getParent(@Nonnull PsiElement element) {
    if (element instanceof PsiFile && InjectedLanguageManager.getInstance(element.getProject()).isInjectedFragment((PsiFile)element)) {
      return element.getParent();
    }
    return element.getContext();
  }

  public Self withElementType(IElementType type) {
    return withElementType(PlatformPatterns.elementType().equalTo(type));
  }

  public Self withElementType(TokenSet type) {
    return withElementType(PlatformPatterns.elementType().tokenSet(type));
  }

  public Self afterLeaf(@Nonnull String... withText) {
    return afterLeaf(PlatformPatterns.psiElement().withText(PlatformPatterns.string().oneOf(withText)));
  }

  public Self afterLeaf(@Nonnull ElementPattern<? extends PsiElement> pattern) {
    return afterLeafSkipping(PlatformPatterns.psiElement().whitespaceCommentEmptyOrError(), pattern);
  }

  public Self beforeLeaf(@Nonnull ElementPattern<? extends PsiElement> pattern) {
    return beforeLeafSkipping(PlatformPatterns.psiElement().whitespaceCommentEmptyOrError(), pattern);
  }

  public Self whitespace() {
    return withElementType(TokenType.WHITE_SPACE);
  }

  public Self whitespaceCommentOrError() {
    return andOr(PlatformPatterns.psiElement().whitespace(), PlatformPatterns.psiElement(PsiComment.class), PlatformPatterns.psiElement(PsiErrorElement.class));
  }

  public Self whitespaceCommentEmptyOrError() {
    return andOr(PlatformPatterns.psiElement().whitespace(), PlatformPatterns.psiElement(PsiComment.class), PlatformPatterns.psiElement(PsiErrorElement.class),
                 PlatformPatterns.psiElement().withText(""));
  }

  public Self withFirstNonWhitespaceChild(@Nonnull ElementPattern<? extends PsiElement> pattern) {
    return withChildren(
            StandardPatterns.collection(PsiElement.class).filter(StandardPatterns.not(PlatformPatterns.psiElement().whitespace()), StandardPatterns.collection(PsiElement.class).first(pattern)));
  }

  public Self withReference(final Class<? extends PsiReference> referenceClass) {
    return with(new PatternCondition<T>("withReference") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        for (PsiReference reference : t.getReferences()) {
          if (referenceClass.isInstance(reference)) {
            return true;
          }
        }
        return false;
      }
    });
  }

  public Self inFile(@Nonnull final ElementPattern<? extends PsiFile> filePattern) {
    return with(new PatternCondition<T>("inFile") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        return filePattern.accepts(t.getContainingFile(), context);
      }
    });
  }

  public Self inVirtualFile(@Nonnull final ElementPattern<? extends VirtualFile> filePattern) {
    return with(new PatternCondition<T>("inVirtualFile") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        return filePattern.accepts(t.getContainingFile().getViewProvider().getVirtualFile(), context);
      }
    });
  }

  @Override
  public Self equalTo(@Nonnull final T o) {
    return with(new PatternCondition<T>("equalTo") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        return t.getManager().areElementsEquivalent(t, o);
      }

    });
  }

  public Self withElementType(final ElementPattern<IElementType> pattern) {
    return with(new PatternCondition<T>("withElementType") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        ASTNode node = t.getNode();
        return node != null && pattern.accepts(node.getElementType());
      }

    });
  }

  public Self withText(@Nonnull @NonNls String text) {
    return withText(StandardPatterns.string().equalTo(text));
  }

  public Self withoutText(@Nonnull String text) {
    return withoutText(StandardPatterns.string().equalTo(text));
  }

  public Self withName(@Nonnull @NonNls String name) {
    return withName(StandardPatterns.string().equalTo(name));
  }

  public Self withName(@Nonnull @NonNls String... names) {
    return withName(StandardPatterns.string().oneOf(names));
  }

  public Self withName(@Nonnull ElementPattern<String> name) {
    return with(new PsiNamePatternCondition<T>("withName", name));
  }

  public Self afterLeafSkipping(@Nonnull final ElementPattern skip, @Nonnull final ElementPattern pattern) {
    return with(new PatternCondition<T>("afterLeafSkipping") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        PsiElement element = t;
        while (true) {
          element = PsiTreeUtil.prevLeaf(element);
          if (element != null && element.getTextLength() == 0) {
            continue;
          }

          if (!skip.getCondition().accepts(element, context)) {
            return pattern.getCondition().accepts(element, context);
          }
        }
      }

    });
  }

  public Self beforeLeafSkipping(@Nonnull final ElementPattern skip, @Nonnull final ElementPattern pattern) {
    return with(new PatternCondition<T>("beforeLeafSkipping") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        PsiElement element = t;
        while (true) {
          element = PsiTreeUtil.nextLeaf(element);
          if (element != null && element.getTextLength() == 0) {
            continue;
          }

          if (!skip.getCondition().accepts(element, context)) {
            return pattern.getCondition().accepts(element, context);
          }
        }
      }

    });
  }

  public Self atStartOf(@Nonnull final ElementPattern pattern) {
    return with(new PatternCondition<T>("atStartOf") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        PsiElement element = t;
        while (element != null) {
          if (pattern.getCondition().accepts(element, context)) {
            return element.getTextRange().getStartOffset() == t.getTextRange().getStartOffset();
          }
          element = element.getContext();
        }
        return false;
      }
    });
  }

  public Self withTextLength(@Nonnull final ElementPattern lengthPattern) {
    return with(new PatternConditionPlus<T, Integer>("withTextLength", lengthPattern) {
      @Override
      public boolean processValues(T t, ProcessingContext context, BiPredicate<Integer, ProcessingContext> integerProcessingContextPairProcessor) {
        return integerProcessingContextPairProcessor.test(t.getTextLength(), context);
      }
    });
  }

  public Self notEmpty() {
    return withTextLengthLongerThan(0);
  }

  public Self withTextLengthLongerThan(final int minLength) {
    return with(new PatternCondition<T>("withTextLengthLongerThan") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        return t.getTextLength() > minLength;
      }
    });
  }

  public Self withText(@Nonnull ElementPattern text) {
    return with(_withText(text));
  }

  private PatternCondition<T> _withText(final ElementPattern pattern) {
    return new PatternConditionPlus<T, String>("_withText", pattern) {
      @Override
      public boolean processValues(T t, ProcessingContext context, BiPredicate<String, ProcessingContext> processor) {
        return processor.test(t.getText(), context);
      }
    };
  }

  public Self withoutText(@Nonnull ElementPattern text) {
    return without(_withText(text));
  }

  public Self withLanguage(@Nonnull final Language language) {
    return with(new PatternCondition<T>("withLanguage") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        return t.getLanguage().equals(language);
      }
    });
  }

  public Self withMetaData(final ElementPattern<? extends PsiMetaData> metaDataPattern) {
    return with(new PatternCondition<T>("withMetaData") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        return t instanceof PsiMetaOwner && metaDataPattern.accepts(((PsiMetaOwner)t).getMetaData(), context);
      }
    });
  }

  public Self referencing(final ElementPattern<? extends PsiElement> targetPattern) {
    return with(new PatternCondition<T>("referencing") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        PsiReference[] references = t.getReferences();
        for (PsiReference reference : references) {
          if (targetPattern.accepts(reference.resolve(), context)) return true;
          if (reference instanceof PsiPolyVariantReference) {
            for (ResolveResult result : ((PsiPolyVariantReference)reference).multiResolve(true)) {
              if (targetPattern.accepts(result.getElement(), context)) return true;
            }
          }
        }
        return false;
      }
    });
  }

  public Self compiled() {
    return with(new PatternCondition<T>("compiled") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        return t instanceof PsiCompiledElement;
      }
    });
  }

  public Self withTreeParent(final ElementPattern<? extends PsiElement> ancestor) {
    return with(new PatternCondition<T>("withTreeParent") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        return ancestor.accepts(t.getParent(), context);
      }
    });
  }

  public Self insideStarting(final ElementPattern<? extends PsiElement> ancestor) {
    return with(new PatternCondition<PsiElement>("insideStarting") {
      @Override
      public boolean accepts(@Nonnull PsiElement start, ProcessingContext context) {
        PsiElement element = getParent(start);
        TextRange range = start.getTextRange();
        if (range == null) return false;

        int startOffset = range.getStartOffset();
        while (element != null && element.getTextRange() != null && element.getTextRange().getStartOffset() == startOffset) {
          if (ancestor.accepts(element, context)) {
            return true;
          }
          element = getParent(element);
        }
        return false;
      }
    });
  }

  public static class Capture<T extends PsiElement> extends PsiElementPattern<T, Capture<T>> {

    protected Capture(Class<T> aClass) {
      super(aClass);
    }

    protected Capture(@Nonnull InitialPatternCondition<T> condition) {
      super(condition);
    }


  }

}
