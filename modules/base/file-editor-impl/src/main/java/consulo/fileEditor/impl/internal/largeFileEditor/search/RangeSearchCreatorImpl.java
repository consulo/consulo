// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.Content;
import consulo.usage.UsageViewContentManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public final class RangeSearchCreatorImpl implements RangeSearchCreator {

    @Override
    public @Nonnull RangeSearch createContent(Project project,
                                              VirtualFile virtualFile,
                                              String titleName) {
        RangeSearchCallback rangeSearchCallback = new RangeSearchCallbackImpl();
        RangeSearch rangeSearch = new RangeSearch(virtualFile, project, rangeSearchCallback);
        Content content = UsageViewContentManager.getInstance(project).addContent(
            titleName, true, rangeSearch.getComponent(), false, true);
        rangeSearch.setContent(content);

        ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.FIND).activate(null, true);

        content.setDisposer(new Disposable() {
            @Override
            public void dispose() {
                rangeSearch.dispose();
            }
        });

        return rangeSearch;
    }
}
