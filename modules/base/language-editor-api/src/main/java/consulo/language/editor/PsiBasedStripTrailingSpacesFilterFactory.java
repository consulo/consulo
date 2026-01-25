/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor;

import consulo.annotation.UsedInPlugin;
import consulo.component.ComponentManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.StripTrailingSpacesFilter;
import consulo.document.StripTrailingSpacesFilterFactory;
import consulo.language.Language;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.util.LanguageUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@UsedInPlugin
public abstract class PsiBasedStripTrailingSpacesFilterFactory extends StripTrailingSpacesFilterFactory {
    private static final Logger LOG = Logger.getInstance(PsiBasedStripTrailingSpacesFilterFactory.class);

    @Override
    @Nonnull
    public final StripTrailingSpacesFilter createFilter(@Nullable ComponentManager project, @Nonnull Document document) {
        Language language = getDocumentLanguage(document);
        if (language != null && isApplicableTo(language)) {
            PsiFile psiFile = getPsiFile((Project) project, document);
            if (psiFile != null) {
                PsiBasedStripTrailingSpacesFilter filter = createFilter(document);
                filter.process(psiFile);
                return filter;
            }
            return StripTrailingSpacesFilter.POSTPONED;
        }
        return StripTrailingSpacesFilter.ALL_LINES;
    }

    @Nullable
    public static Language getDocumentLanguage(@Nonnull Document document) {
        FileDocumentManager manager = FileDocumentManager.getInstance();
        VirtualFile file = manager.getFile(document);
        if (file != null && file.isValid()) {
            return LanguageUtil.getFileLanguage(file);
        }
        return null;
    }

    private static @Nullable PsiFile getPsiFile(@Nullable Project project, @Nonnull Document document) {
        if (project != null) {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            if (documentManager.isCommitted(document)) {
                return documentManager.getCachedPsiFile(document);
            }
        }
        else {
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            LOG.warn(
                "No current project is given, trailing spaces will be stripped later (postponed). File: " +
                    (virtualFile != null ? virtualFile.getCanonicalPath() : "undefined"));
        }
        return null;
    }

    @Nonnull
    protected abstract PsiBasedStripTrailingSpacesFilter createFilter(@Nonnull Document document);

    protected abstract boolean isApplicableTo(@Nonnull Language language);
}