// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.diff.DiffEditorTabFilesManager;
import consulo.diff.combined.CombinedBlockProducer;
import consulo.diff.combined.CombinedDiffVirtualFileImpl;
import consulo.diff.combined.CombinedPathBlockId;
import consulo.diff.combined.CombinedDiffToggle;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.diff.ChangeDiffRequestProducer;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.util.ArrayList;
import java.util.List;

@ActionImpl(id = "Vcs.Diff.ShowCombinedDiff")
public class ShowCombinedDiffAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public ShowCombinedDiffAction() {
        super(VcsLocalize.actionShowCombinedDiffText(), VcsLocalize.actionShowCombinedDiffDescription(), PlatformIconGroup.actionsDiff());
    }

    @Override
    public void update(AnActionEvent e) {
        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        Project project = e.getData(Project.KEY);

        e.getPresentation().setEnabledAndVisible(CombinedDiffToggle.DEFAULT.isCombinedDiffEnabled()
            && project != null && changes != null && changes.length > 1);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        if (project == null || changes == null) {
            return;
        }

        showDiff(project, List.of(changes));
    }

    @RequiredUIAccess
    public static void showDiff(Project project, List<Change> changes) {
        List<CombinedBlockProducer> producers = new ArrayList<>();
        for (Change change : changes) {
            ChangeDiffRequestProducer producer = ChangeDiffRequestProducer.create(project, change);
            if (producer == null) {
                continue;
            }
            CombinedPathBlockId id = new CombinedPathBlockId(producer.getFilePath(), change.getFileStatus());
            producers.add(new CombinedBlockProducer(id, producer));
        }

        CombinedDiffVirtualFileImpl allInOneDiffFile =
            new CombinedDiffVirtualFileImpl(project, producers, VcsLocalize.changesCombinedDiff().get());

        DiffEditorTabFilesManager.getInstance(project).showDiffFile(allInOneDiffFile, true);
    }
}
