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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.FileStatusMap;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Headless {@code DaemonCodeAnalyzer}: the production {@code DaemonCodeAnalyzerImpl} lives in
 * {@code ide-impl}. The daemon never runs; everything reports "idle and finished". Binding it lets
 * the real {@code DaemonListeners} (language-editor-impl, {@code lazy = false}) construct at project
 * startup. Bound only under the {@link ComponentProfiles#INTEGRATION_TEST} profile.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessDaemonCodeAnalyzer extends DaemonCodeAnalyzerInternal {
    /**
     * All-clean {@link FileStatusMap}: nothing is ever dirty and no errors were found.
     */
    private static final class HeadlessFileStatusMap implements FileStatusMap {
        @Override
        public @Nullable TextRange getFileDirtyScope(Document document, int passId) {
            return null;
        }

        @Override
        public void setErrorFoundFlag(Project project, Document document, boolean errorFound) {
        }

        @Override
        public void markFileUpToDate(Document document, int passId) {
        }

        @Override
        public boolean wasErrorFound(Document document) {
            return false;
        }

        @Override
        public void markAllFilesDirty(Object reason) {
        }

        @Override
        public void markFileScopeDirtyDefensively(PsiFile file, Object reason) {
        }

        @Override
        public void markFileScopeDirty(Document document, TextRange scope, int fileLength, Object reason) {
        }

        @Override
        public boolean allDirtyScopesAreNull(Document document) {
            return true;
        }
    }

    private final FileStatusMap myFileStatusMap = new HeadlessFileStatusMap();

    @Inject
    public HeadlessDaemonCodeAnalyzer(ActionManager actionManager) {
        // DaemonListeners (constructed right after this service) resolves these editor actions
        // eagerly in its constructor; nothing registers actions in the headless harness, so provide stubs
        registerStubAction(actionManager, IdeActions.ACTION_EDITOR_CUT, "Cut");
        registerStubAction(actionManager, IdeActions.ACTION_EDITOR_ESCAPE, "Escape");
    }

    private static void registerStubAction(ActionManager actionManager, String actionId, String text) {
        if (actionManager.getAction(actionId) != null) {
            return;
        }

        actionManager.registerAction(actionId, new AnAction(LocalizeValue.of(text)) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
            }
        });
    }

    @Override
    public void settingsChanged() {
    }

    @Override
    public void setUpdateByTimerEnabled(boolean value) {
    }

    @Override
    public void disableUpdateByTimer(Disposable parentDisposable) {
    }

    @Override
    public boolean isHighlightingAvailable(@Nullable PsiFile file) {
        return false;
    }

    @Override
    public void setImportHintsEnabled(PsiFile file, boolean value) {
    }

    @Override
    public void resetImportHintsEnabledForProject() {
    }

    @Override
    public void setHighlightingEnabled(PsiFile file, boolean value) {
    }

    @Override
    public boolean isImportHintsEnabled(PsiFile file) {
        return false;
    }

    @Override
    public boolean isAutohintsAvailable(@Nullable PsiFile file) {
        return false;
    }

    @Override
    public ProgressIndicator createDaemonProgressIndicator() {
        return new EmptyProgressIndicator();
    }

    @Override
    public void restart() {
    }

    @Override
    public void restart(PsiFile file) {
    }

    @Override
    public void autoImportReferenceAtCursor(Editor editor, PsiFile file) {
    }

    @Override
    public FileStatusMap getFileStatusMap() {
        return myFileStatusMap;
    }

    @Override
    public boolean isErrorAnalyzingFinished(PsiFile file) {
        return true;
    }

    @Override
    public List<HighlightInfo> runMainPasses(PsiFile psiFile, Document document, ProgressIndicator progress) {
        return List.of();
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public List<HighlightInfo> getFileLevelHighlights(Project project, PsiFile file) {
        return List.of();
    }

    @Override
    public void cleanFileLevelHighlights(Project project, int group, PsiFile psiFile) {
    }

    @Override
    public void addFileLevelHighlight(Project project, int group, HighlightInfo info, PsiFile psiFile) {
    }

    @Override
    public void removeFileLevelHighlight(Project project, HighlightInfo info, PsiFile psiFile) {
    }

    @Override
    public boolean doRestart() {
        return false;
    }

    @Override
    public boolean stopProcess(boolean toRestartAlarm, String reason) {
        return false;
    }
}
