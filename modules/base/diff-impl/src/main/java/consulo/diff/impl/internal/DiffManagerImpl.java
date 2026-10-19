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
package consulo.diff.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffEditorTabFilesManager;
import consulo.diff.DiffRequestPanel;
import consulo.diff.DiffTool;
import consulo.diff.DiffWindowMode;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.chain.SimpleDiffRequestChain;
import consulo.diff.impl.internal.editor.ChainDiffVirtualFile;
import consulo.diff.impl.internal.external.ExternalDiffTool;
import consulo.diff.impl.internal.external.ExternalMergeTool;
import consulo.diff.impl.internal.merge.MergeWindow;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.internal.DiffManagerEx;
import consulo.diff.internal.DiffSettingsHolder;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeTool;
import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Singleton
@ServiceImpl
public class DiffManagerImpl extends DiffManagerEx {
    private final Application myApplication;

    @Inject
    public DiffManagerImpl(Application application) {
        myApplication = application;
    }

    @Override
    @RequiredUIAccess
    public void showDiff(@Nullable Project project, DiffRequest request) {
        showDiff(project, request, DiffDialogHints.DEFAULT);
    }

    @Override
    @RequiredUIAccess
    public void showDiff(@Nullable Project project, DiffRequest request, DiffDialogHints hints) {
        DiffRequestChain requestChain = new SimpleDiffRequestChain(request);
        showDiff(project, requestChain, hints);
    }

    @Override
    @RequiredUIAccess
    public void showDiff(@Nullable Project project, DiffRequestChain requests, DiffDialogHints hints) {
        if (ExternalDiffTool.isDefault()) {
            ExternalDiffTool.show(project, requests, hints);
            return;
        }

        showDiffBuiltin(project, requests, hints);
    }

    @Override
    @RequiredUIAccess
    public void showDiffBuiltin(@Nullable Project project, DiffRequest request) {
        showDiffBuiltin(project, request, DiffDialogHints.DEFAULT);
    }

    @Override
    @RequiredUIAccess
    public void showDiffBuiltin(@Nullable Project project, DiffRequest request, DiffDialogHints hints) {
        DiffRequestChain requestChain = new SimpleDiffRequestChain(request);
        showDiffBuiltin(project, requestChain, hints);
    }

    @Override
    @RequiredUIAccess
    public void showDiffBuiltin(@Nullable Project project, DiffRequestChain requests, DiffDialogHints hints) {
        DiffEditorTabFilesManager diffEditorTabFilesManager = project != null ? DiffEditorTabFilesManager.getInstance(project) : null;
        if (diffEditorTabFilesManager != null
            && DiffSettingsHolder.DiffSettings.getSettings().isShowDiffInEditor()
            && UnifiedDiffUtil.getWindowMode(hints) == DiffWindowMode.FRAME
            && !UIAccess.current().isInModalContext()) {
            ChainDiffVirtualFile diffFile = new ChainDiffVirtualFile(requests, DiffLocalize.labelDefaultDiffEditorTabName().get());
            diffEditorTabFilesManager.showDiffFile(diffFile, true);
            return;
        }

        new DiffWindow(project, requests, hints).show();
    }

    @Override
    @RequiredUIAccess
    public DiffRequestPanel createRequestPanel(@Nullable Project project, Disposable parent, consulo.ui.@Nullable Window window) {
        DiffRequestPanelImpl panel = new DiffRequestPanelImpl(project, window);
        Disposer.register(parent, panel);
        return panel;
    }

    @Override
    public List<DiffTool> getDiffTools() {
        return myApplication.getExtensionList(DiffTool.class);
    }

    
    @Override
    public List<MergeTool> getMergeTools() {
        return myApplication.getExtensionList(MergeTool.class);
    }

    @Override
    @RequiredUIAccess
    public void showMerge(@Nullable Project project, MergeRequest request) {
        if (ExternalMergeTool.isDefault()) {
            ExternalMergeTool.show(project, request);
            return;
        }

        showMergeBuiltin(project, request);
    }

    @Override
    @RequiredUIAccess
    public void showMergeBuiltin(@Nullable Project project, MergeRequest request) {
        new MergeWindow(project, request).show();
    }
}
