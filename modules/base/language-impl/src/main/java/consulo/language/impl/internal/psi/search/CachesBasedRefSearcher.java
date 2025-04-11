// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi.search;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.function.Processor;
import consulo.content.scope.SearchScope;
import consulo.language.impl.psi.SyntheticFileSystemItem;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.meta.PsiMetaData;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.project.util.query.QueryExecutorBase;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * @author max
 */
@ExtensionImpl
public class CachesBasedRefSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> implements ReferencesSearchQueryExecutor {
    public CachesBasedRefSearcher() {
        super(true);
    }

    @Override
    @RequiredReadAction
    public void processQuery(@Nonnull ReferencesSearch.SearchParameters p, @Nonnull Predicate<? super PsiReference> consumer) {
        PsiElement refElement = p.getElementToSearch();
        boolean caseSensitive = refElement.getLanguage().isCaseSensitive();

        String text = null;
        if (refElement instanceof PsiFileSystemItem fileSystemItem && !(refElement instanceof SyntheticFileSystemItem)) {
            VirtualFile vFile = fileSystemItem.getVirtualFile();
            if (vFile != null) {
                String fileNameWithoutExtension = vFile.getNameWithoutExtension();
                text = fileNameWithoutExtension.isEmpty() ? vFile.getName() : fileNameWithoutExtension;
            }
            // We must not look for file references with the file language's case-sensitivity,
            // since case-sensitivity of the references themselves depends either on file system
            // or on the rules of the language of reference
            caseSensitive = false;
        }
        else if (refElement instanceof PsiNamedElement namedElement) {
            text = namedElement.getName();
            if (refElement instanceof PsiMetaOwner metaOwner) {
                PsiMetaData metaData = metaOwner.getMetaData();
                if (metaData != null) {
                    text = metaData.getName();
                }
            }
        }

        if (text == null && refElement instanceof PsiMetaOwner psiMetaOwner) {
            PsiMetaData metaData = psiMetaOwner.getMetaData();
            if (metaData != null) {
                text = metaData.getName();
            }
        }
        if (StringUtil.isNotEmpty(text)) {
            SearchScope searchScope = p.getEffectiveSearchScope();
            p.getOptimizer().searchWord(text, searchScope, caseSensitive, refElement);
        }
    }
}