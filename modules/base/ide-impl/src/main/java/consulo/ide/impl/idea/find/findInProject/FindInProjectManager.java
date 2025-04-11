// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.find.findInProject;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.DataContext;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.ide.impl.idea.find.impl.FindManagerImpl;
import consulo.ide.impl.idea.find.replaceInProject.ReplaceInProjectManager;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.usage.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.function.Predicate;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class FindInProjectManager {
    private final Project myProject;
    private volatile boolean myIsFindInProgress;

    public static FindInProjectManager getInstance(Project project) {
        return ServiceManager.getService(project, FindInProjectManager.class);
    }

    @Inject
    public FindInProjectManager(Project project) {
        myProject = project;
    }

    /**
     * @param model would be used for search if not null, otherwise shared (project-level) model would be used
     */
    @RequiredUIAccess
    public void findInProject(@Nonnull DataContext dataContext, @Nullable FindModel model) {
        FindManager findManager = FindManager.getInstance(myProject);
        FindModel findModel;
        if (model != null) {
            findModel = model.clone();
        }
        else {
            findModel = findManager.getFindInProjectModel().clone();
            findModel.setReplaceState(false);
            initModel(findModel, dataContext);
        }

        findManager.showFindDialog(
            findModel,
            () -> {
                if (findModel.isReplaceState()) {
                    ReplaceInProjectManager.getInstance(myProject).replaceInPath(findModel);
                }
                else {
                    findInPath(findModel);
                }
            }
        );
    }

    public void findInPath(@Nonnull FindModel findModel) {
        startFindInProject(findModel);
    }

    @SuppressWarnings("WeakerAccess")
    protected void initModel(@Nonnull FindModel findModel, @Nonnull DataContext dataContext) {
        FindInProjectUtil.setDirectoryName(findModel, dataContext);

        String text = dataContext.getData(PlatformDataKeys.PREDEFINED_TEXT);
        if (text != null) {
            FindModel.initStringToFindNoMultiline(findModel, text);
        }
        else {
            FindInProjectUtil.initStringToFindFromDataContext(findModel, dataContext);
        }
    }

    public void startFindInProject(@Nonnull FindModel findModel) {
        if (findModel.getDirectoryName() != null && FindInProjectUtil.getDirectory(findModel) == null) {
            return;
        }

        UsageViewManager manager = UsageViewManager.getInstance(myProject);

        if (manager == null) {
            return;
        }
        FindManager findManager = FindManager.getInstance(myProject);
        findManager.getFindInProjectModel().copyFrom(findModel);
        FindModel findModelCopy = findModel.clone();
        UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(findModelCopy);
        FindUsagesProcessPresentation processPresentation = FindInProjectUtil.setupProcessPresentation(myProject, presentation);
        ConfigurableUsageTarget usageTarget = new FindInProjectUtil.StringUsageTarget(myProject, findModel);

        ((FindManagerImpl)FindManager.getInstance(myProject)).getFindUsagesManager().addToHistory(usageTarget);

        manager.searchAndShowUsages(
            new UsageTarget[]{usageTarget},
            () -> processor -> {
                myIsFindInProgress = true;

                try {
                    Predicate<UsageInfo> consumer = info -> {
                        Usage usage = UsageInfo2UsageAdapter.CONVERTER.apply(info);
                        usage.getPresentation().getIcon(); // cache icon
                        return processor.test(usage);
                    };
                    FindInProjectUtil.findUsages(findModelCopy, myProject, consumer, processPresentation);
                }
                finally {
                    myIsFindInProgress = false;
                }
            },
            processPresentation,
            presentation,
            null
        );
    }

    public boolean isWorkInProgress() {
        return myIsFindInProgress;
    }

    public boolean isEnabled() {
        return !myIsFindInProgress && !ReplaceInProjectManager.getInstance(myProject).isWorkInProgress();
    }
}
