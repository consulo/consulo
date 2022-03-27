// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.CachedValueProvider;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author peter
 */
final class CachedValueLeakChecker {
  private static final Logger LOG = Logger.getInstance(CachedValueLeakChecker.class);
  private static final boolean DO_CHECKS = ApplicationManager.getApplication().isUnitTestMode();
  private static final Set<String> ourCheckedKeys = ContainerUtil.newConcurrentSet();

  static void checkProvider(@Nonnull CachedValueProvider<?> provider, @Nonnull Key<?> key, @Nonnull UserDataHolder userDataHolder) {
    if (!DO_CHECKS) return;
    if (!ourCheckedKeys.add(key.toString())) return; // store strings because keys are created afresh in each (test) project

    findReferencedPsi(provider, key, userDataHolder);
  }

  private static synchronized void findReferencedPsi(@Nonnull Object root, @Nonnull Key<?> key, @Nonnull UserDataHolder toIgnore) {
    Predicate<Object> shouldExamineValue = value -> {
      if (value == toIgnore) return false;
      if (value instanceof ASTNode) {
        value = ((ASTNode)value).getPsi();
        if (value == toIgnore) return false;
      }
      if (value instanceof Project || value instanceof Module || value instanceof Application) return false;
      if (value instanceof PsiElement &&
          toIgnore instanceof PsiElement &&
          ((PsiElement)toIgnore).getContainingFile() != null &&
          PsiTreeUtil.isAncestor((PsiElement)value, (PsiElement)toIgnore, true)) {
        // allow to capture PSI parents, assuming that they stay valid at least as long as the element itself
        return false;
      }
      return true;
    };
    Map<Object, String> roots = Collections.singletonMap(root, "CachedValueProvider " + key);
    DebugReflectionUtil.walkObjects(5, roots, PsiElement.class, shouldExamineValue, (value, backLink) -> {
      if (value instanceof PsiElement) {
        LOG.error("Incorrect CachedValue use. Provider references PSI, causing memory leaks and possible invalid element access, provider=" + root + "\n" + backLink);
        return false;
      }
      return true;
    });
  }
}
