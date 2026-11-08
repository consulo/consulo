// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.dataFlow.ir;

import consulo.language.dataFlow.value.DfaValueFactory;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.PsiModificationTracker;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A language extension point that builds {@link ControlFlow} for specific language
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DataFlowIRProvider extends LanguageExtension {
  ExtensionPointCacheKey<DataFlowIRProvider, ByLanguageValue<DataFlowIRProvider>> KEY =
    ExtensionPointCacheKey.create("DataFlowIRProvider", LanguageOneToOne.build());

  static @Nullable DataFlowIRProvider forLanguage(Language language) {
    return Application.get().getExtensionPoint(DataFlowIRProvider.class).getOrBuildCache(KEY).get(language);
  }

  /**
   * Create control flow for given PSI block (method body, lambda expression, etc.) and return it.
   * It's prohibited to change the resulting control flow (e.g. add instructions, update their indices, update flush variable lists, etc.)
   *
   * @param psiBlock psi-block
   * @param factory factory to bind the PSI block to
   * @return resulting control flow; null if it cannot be built (e.g. if the code block contains unrecoverable errors)
   */
  @Nullable ControlFlow createControlFlow(DfaValueFactory factory, PsiElement psiBlock);

  /**
   * @param startAnchor         an anchor returned from {@link DfaAssistProvider#getAnchor(PsiElement)}, which created this listener
   * @param unreachableElements list of all {@link PsiElement} which were never visited during IR interpretation. This means that no
   *                            instruction between {@link ControlFlow#startElement(PsiElement)} and
   *                            {@link ControlFlow#finishElement(PsiElement)} was reached. Elements before startAnchor in IR are not
   *                            considered unreachable and not added to this set.
   * @return collection of text ranges to highlight as unreachable.
   */
  default Collection<TextRange> unreachableSegments(PsiElement startAnchor, Set<PsiElement> unreachableElements) {
    return Collections.emptyList();
  }

  /**
   * Create control flow for given PSI block (method body, lambda expression, etc.) and return it. May return cached block.
   * It's prohibited to change the resulting control flow (e.g. add instructions, update their indices, update flush variable lists, etc.)
   *
   * @param psiBlock psi-block
   * @param targetFactory factory to bind the PSI block to
   * @return resulting control flow; null if it cannot be built (e.g. unsupported language, code block contains unrecoverable errors, etc.)
   */
  static @Nullable ControlFlow forElement(PsiElement psiBlock, DfaValueFactory targetFactory) {
    PsiFile file = psiBlock.getContainingFile();
    DataFlowIRProvider provider = forLanguage(file.getLanguage());
    if (provider == null) {
      return null;
    }
    ConcurrentHashMap<PsiElement, Optional<ControlFlow>> fileMap =
      LanguageCachedValueUtil.getCachedValue(file, () ->
        CachedValueProvider.Result.create(new ConcurrentHashMap<>(), PsiModificationTracker.MODIFICATION_COUNT));
    return fileMap.computeIfAbsent(psiBlock, psi -> {
      DfaValueFactory factory = new DfaValueFactory(file.getProject());
      ControlFlow flow = provider.createControlFlow(factory, psiBlock);
      return Optional.ofNullable(flow);
    }).map(flow -> new ControlFlow(flow, targetFactory)).orElse(null);
  }
  
  static Collection<TextRange> computeUnreachableSegments(PsiElement startAnchor, Set<PsiElement> unreachableElements) {
    PsiFile file = startAnchor.getContainingFile();
    DataFlowIRProvider provider = forLanguage(file.getLanguage());
    if (provider == null) {
      return Collections.emptyList();
    }
    return provider.unreachableSegments(startAnchor, unreachableElements);
  }
}
