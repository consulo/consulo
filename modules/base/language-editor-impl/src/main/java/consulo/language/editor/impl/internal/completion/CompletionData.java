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

import consulo.application.dumb.IndexNotReadyException;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.lookup.*;
import consulo.language.editor.template.Template;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.ObjectPattern;
import consulo.language.plain.psi.PsiPlainText;
import consulo.language.psi.*;
import consulo.language.psi.filter.ElementFilter;
import consulo.language.psi.filter.TrueFilter;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.path.PsiDynaReference;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

import static consulo.language.pattern.StandardPatterns.character;
import static consulo.language.pattern.StandardPatterns.not;

/**
 * @deprecated see {@link CompletionContributor}
 */
@SuppressWarnings("deprecation")
public class CompletionData {
  private static final Logger LOG = Logger.getInstance(CompletionData.class);
  public static final ObjectPattern.Capture<Character> NOT_JAVA_ID = not(character().javaIdentifierPart());
  private final List<CompletionVariant> myCompletionVariants = new ArrayList<>();

  protected CompletionData() {
  }

  private boolean isScopeAcceptable(PsiElement scope) {

    for (CompletionVariant variant : myCompletionVariants) {
      if (variant.isScopeAcceptable(scope)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @see CompletionContributor
   * @deprecated
   */
  protected void registerVariant(CompletionVariant variant) {
    myCompletionVariants.add(variant);
  }

  public void completeReference(PsiReference reference, Set<LookupElement> set, @Nonnull PsiElement position, PsiFile file) {
    CompletionVariant[] variants = findVariants(position, file);
    boolean hasApplicableVariants = false;
    for (CompletionVariant variant : variants) {
      if (variant.hasReferenceFilter()) {
        variant.addReferenceCompletions(reference, position, set, file, this);
        hasApplicableVariants = true;
      }
    }

    if (!hasApplicableVariants) {
      myGenericVariant.addReferenceCompletions(reference, position, set, file, this);
    }
  }

  public void addKeywordVariants(Set<CompletionVariant> set, PsiElement position, PsiFile file) {
    ContainerUtil.addAll(set, findVariants(position, file));
  }

  public void completeKeywordsBySet(Set<LookupElement> set, Set<CompletionVariant> variants) {
    for (CompletionVariant variant : variants) {
      variant.addKeywords(set, this);
    }
  }

  public String findPrefix(PsiElement insertedElement, int offsetInFile) {
    return findPrefixStatic(insertedElement, offsetInFile);
  }

  public CompletionVariant[] findVariants(PsiElement position, PsiFile file) {
    List<CompletionVariant> variants = new ArrayList<>();
    PsiElement scope = position;
    if (scope == null) {
      scope = file;
    }
    while (scope != null) {
      boolean breakFlag = false;
      if (isScopeAcceptable(scope)) {

        for (CompletionVariant variant : myCompletionVariants) {
          if (variant.isVariantApplicable(position, scope) && !variants.contains(variant)) {
            variants.add(variant);
            if (variant.isScopeFinal(scope)) {
              breakFlag = true;
            }
          }
        }
      }
      if (breakFlag) break;
      scope = scope.getContext();
      if (scope instanceof PsiDirectory) break;
    }
    return variants.toArray(new CompletionVariant[variants.size()]);
  }

  protected final CompletionVariant myGenericVariant = new CompletionVariant() {
    @Override
    void addReferenceCompletions(PsiReference reference, PsiElement position, Set<LookupElement> set, PsiFile file, CompletionData completionData) {
      completeReference(reference, position, set, TailType.NONE, TrueFilter.INSTANCE, this);
    }
  };

  @Nullable
  public static String getReferencePrefix(@Nonnull PsiElement insertedElement, int offsetInFile) {
    try {
      PsiReference ref = insertedElement.getContainingFile().findReferenceAt(offsetInFile);
      if (ref != null) {
        List<TextRange> ranges = ReferenceRange.getRanges(ref);
        PsiElement element = ref.getElement();
        int elementStart = element.getTextRange().getStartOffset();
        for (TextRange refRange : ranges) {
          if (refRange.contains(offsetInFile - elementStart)) {
            int endIndex = offsetInFile - elementStart;
            int beginIndex = refRange.getStartOffset();
            if (beginIndex > endIndex) {
              LOG.error("Inconsistent reference (found at offset not included in its range): ref=" + ref + " element=" + element + " text=" + element.getText());
            }
            if (beginIndex < 0) {
              LOG.error("Inconsistent reference (begin < 0): ref=" + ref + " element=" + element + "; begin=" + beginIndex + " text=" + element.getText());
            }
            LOG.assertTrue(endIndex >= 0);
            return element.getText().substring(beginIndex, endIndex);
          }
        }
      }
    }
    catch (IndexNotReadyException ignored) {
    }
    return null;
  }

  public static String findPrefixStatic(PsiElement insertedElement, int offsetInFile, ElementPattern<Character> prefixStartTrim) {
    if (insertedElement == null) return "";

    Document document = insertedElement.getContainingFile().getViewProvider().getDocument();
    assert document != null;
    LOG.assertTrue(!PsiDocumentManager.getInstance(insertedElement.getProject()).isUncommited(document), "Uncommitted");

    String prefix = getReferencePrefix(insertedElement, offsetInFile);
    if (prefix != null) return prefix;

    if (insertedElement instanceof PsiPlainText || insertedElement instanceof PsiComment) {
      return CompletionUtil.findJavaIdentifierPrefix(insertedElement, offsetInFile);
    }

    return findPrefixDefault(insertedElement, offsetInFile, prefixStartTrim);
  }

  public static String findPrefixStatic(PsiElement insertedElement, int offsetInFile) {
    return findPrefixStatic(insertedElement, offsetInFile, NOT_JAVA_ID);
  }

  public static String findPrefixDefault(PsiElement insertedElement, int offset, @Nonnull ElementPattern trimStart) {
    String substr = insertedElement.getText().substring(0, offset - insertedElement.getTextRange().getStartOffset());
    if (substr.length() == 0 || Character.isWhitespace(substr.charAt(substr.length() - 1))) return "";

    substr = substr.trim();

    int i = 0;
    while (substr.length() > i && trimStart.accepts(substr.charAt(i))) i++;
    return substr.substring(i).trim();
  }

  public static LookupElement objectToLookupItem(@Nonnull Object object) {
    if (object instanceof LookupElement) return (LookupElement)object;

    String s = null;
    TailType tailType = TailType.NONE;
    if (object instanceof PsiElement) {
      s = PsiUtilCore.getName((PsiElement)object);
    }
    else if (object instanceof PsiMetaData) {
      s = ((PsiMetaData)object).getName();
    }
    else if (object instanceof String) {
      s = (String)object;
    }
    else if (object instanceof Template) {
      s = ((Template)object).getKey();
    }
    else if (object instanceof PresentableLookupValue) {
      s = ((PresentableLookupValue)object).getPresentation();
    }
    if (s == null) {
      throw new AssertionError("Null string for object: " + object + " of class " + object.getClass());
    }

    LookupItem item = new LookupItem(object, s);

    if (object instanceof LookupValueWithUIHint && ((LookupValueWithUIHint)object).isBold()) {
      item.setBold();
    }
    item.setAttribute(LookupItem.TAIL_TYPE_ATTR, tailType);
    return item;
  }


  protected void addLookupItem(Set<LookupElement> set, TailType tailType, @Nonnull Object completion, CompletionVariant variant) {
    LookupElement ret = objectToLookupItem(completion);
    if (ret == null) return;
    if (!(ret instanceof LookupItem)) {
      set.add(ret);
      return;
    }

    LookupItem item = (LookupItem)ret;

    InsertHandler insertHandler = variant.getInsertHandler();
    if (insertHandler != null && item.getInsertHandler() == null) {
      item.setInsertHandler(insertHandler);
      item.setTailType(TailType.UNKNOWN);
    }
    else if (tailType != TailType.NONE) {
      item.setTailType(tailType);
    }
    Map<Object, Object> itemProperties = variant.getItemProperties();
    for (Object key : itemProperties.keySet()) {
      item.setAttribute(key, itemProperties.get(key));
    }

    set.add(ret);
  }

  protected void completeReference(PsiReference reference, PsiElement position, Set<LookupElement> set, TailType tailType, ElementFilter filter, CompletionVariant variant) {
    if (reference instanceof PsiMultiReference) {
      for (PsiReference ref : getReferences((PsiMultiReference)reference)) {
        completeReference(ref, position, set, tailType, filter, variant);
      }
    }
    else if (reference instanceof PsiDynaReference) {
      for (PsiReference ref : ((PsiDynaReference<?>)reference).getReferences()) {
        completeReference(ref, position, set, tailType, filter, variant);
      }
    }
    else {
      Object[] completions = reference.getVariants();
      for (Object completion : completions) {
        if (completion == null) {
          LOG.error("Position2D=" + position + "\n;Reference=" + reference + "\n;variants=" + Arrays.toString(completions));
          continue;
        }
        if (completion instanceof PsiElement) {
          PsiElement psiElement = (PsiElement)completion;
          if (filter.isClassAcceptable(psiElement.getClass()) && filter.isAcceptable(psiElement, position)) {
            addLookupItem(set, tailType, completion, variant);
          }
        }
        else {
          if (completion instanceof LookupItem) {
            Object o = ((LookupItem)completion).getObject();
            if (o instanceof PsiElement) {
              if (!filter.isClassAcceptable(o.getClass()) || !filter.isAcceptable(o, position)) continue;
            }
          }
          try {
            addLookupItem(set, tailType, completion, variant);
          }
          catch (AssertionError e) {
            LOG.error("Caused by variant from reference: " + reference.getClass(), e);
          }
        }
      }
    }
  }

  public static PsiReference[] getReferences(PsiMultiReference multiReference) {
    PsiReference[] references = multiReference.getReferences();
    List<PsiReference> hard = ContainerUtil.findAll(references, object -> !object.isSoft());
    if (!hard.isEmpty()) {
      return hard.toArray(new PsiReference[hard.size()]);
    }
    return references;
  }

  void addKeywords(Set<LookupElement> set, CompletionVariant variant, Object comp, TailType tailType) {
    if (!(comp instanceof String)) return;

    for (LookupElement item : set) {
      if (item.getObject().toString().equals(comp)) {
        return;
      }
    }
    addLookupItem(set, tailType, comp, variant);
  }
}
