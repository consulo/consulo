/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Manages the background highlighting and auto-import for files displayed in editors.
 */
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public abstract class DaemonCodeAnalyzer {
    
    public static DaemonCodeAnalyzer getInstance(Project project) {
        return project.getComponent(DaemonCodeAnalyzer.class);
    }

    public abstract void settingsChanged();

    public abstract void setUpdateByTimerEnabled(boolean value);

    public abstract void disableUpdateByTimer(Disposable parentDisposable);

    public abstract boolean isHighlightingAvailable(@Nullable PsiFile file);

    public abstract void setImportHintsEnabled(PsiFile file, boolean value);

    public abstract void resetImportHintsEnabledForProject();

    public abstract void setHighlightingEnabled(PsiFile file, boolean value);

    public abstract boolean isImportHintsEnabled(PsiFile file);

    public abstract boolean isAutohintsAvailable(@Nullable PsiFile file);

    
    public abstract ProgressIndicator createDaemonProgressIndicator();

    /**
     * Force re-highlighting for all files.
     */
    public void restart(String reason) {
        // debug reason not supported
        restart();
    }

    /**
     * Force re-highlighting for all files.
     */
    public abstract void restart();

    /**
     * Force re-highlighting for a specific file.
     *
     * @param file the file to re-highlight.
     */
    public abstract void restart(PsiFile file);

    public abstract void autoImportReferenceAtCursor(Editor editor, PsiFile file);

    
    public abstract FileStatusMap getFileStatusMap();

    public abstract boolean isErrorAnalyzingFinished(PsiFile file);

    /**
     * @param document
     * @param project
     * @param minSeverity null means all
     * @param startOffset
     * @param endOffset
     * @param processor
     * @return
     */
    @RequiredReadAction
    public static boolean processHighlights(
        Document document,
        Project project,
        @Nullable HighlightSeverity minSeverity,
        int startOffset,
        int endOffset,
        Predicate<HighlightInfo> processor
    ) {
        Application.get().assertReadAccessAllowed();

        SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
        MarkupModelEx model = DocumentMarkupModel.forDocument(document, project, true);
        return model.processRangeHighlightersOverlappingWith(
            startOffset,
            endOffset,
            marker -> {
                //noinspection SimplifiableIfStatement
                if (!(marker.getErrorStripeTooltip() instanceof HighlightInfo info)) {
                    return true;
                }
                return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0
                    || info.getHighlighter() == null
                    || processor.test(info);
            }
        );
    }

    /**
     * @param document
     * @param project
     * @param minSeverity null means all
     * @param startOffset
     * @param endOffset
     * @param processor
     * @return
     */
    @RequiredReadAction
    public static boolean processHighlightsOverlappingOutside(
        Document document,
        Project project,
        @Nullable HighlightSeverity minSeverity,
        int startOffset,
        int endOffset,
        Predicate<HighlightInfo> processor
    ) {
        Application.get().assertReadAccessAllowed();

        SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(project);
        MarkupModelEx model = DocumentMarkupModel.forDocument(document, project, true);
        return model.processRangeHighlightersOutside(
            startOffset,
            endOffset,
            marker -> {
                //noinspection SimplifiableIfStatement
                if (!(marker.getErrorStripeTooltip() instanceof HighlightInfo info)) {
                    return true;
                }
                return minSeverity != null && severityRegistrar.compare(info.getSeverity(), minSeverity) < 0
                    || info.getHighlighter() == null
                    || processor.test(info);
            }
        );
    }

    @RequiredReadAction
    public static boolean hasErrors(Project project, Document document) {
        return !processHighlights(document, project, HighlightSeverity.ERROR, 0, document.getTextLength(), (i) -> false);
    }
}
