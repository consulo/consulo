/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.application.dumb.DumbAware;
import consulo.application.util.UserHomeFileUtil;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.impl.internal.ProjectImplUtil;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.localize.ProjectLocalize;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameState;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.InputEvent;
import java.io.File;
import java.util.List;

/**
 * @author yole
 */
public class ReopenProjectAction extends AnAction implements DumbAware {
    private final String myProjectPath;
    private final String myProjectName;
    private List<String> myExtensions;
    @Nullable
    private final IdeFrameState myFrameState;
    private final boolean myOpened;
    private boolean myIsRemoved;

    private final LocalizeValue myProjectText;

    public ReopenProjectAction(final String projectPath,
                               final String projectName,
                               final String displayName,
                               @Nonnull List<String> extensions,
                               @Nullable IdeFrameState frameState,
                               boolean opened) {
        myProjectPath = projectPath;
        myProjectName = projectName;
        myExtensions = extensions;
        myFrameState = frameState;
        myOpened = opened;

        final Presentation presentation = getTemplatePresentation();
        String text = projectPath.equals(displayName) ? UserHomeFileUtil.getLocationRelativeToUserHome(projectPath) : displayName;

        myProjectText = LocalizeValue.of(text);

        presentation.setDisabledMnemonic(true);
        presentation.setTextValue(myProjectText);
        presentation.setDescriptionValue(LocalizeValue.of(projectPath));
        presentation.setIcon(getExtensionIcon());
        presentation.setEnabled(!opened);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (myOpened) {
            presentation.setEnabled(false);
            presentation.setTextValue(ProjectLocalize.recentProject0OpenedActionText(myProjectText));
        } else {
            presentation.setEnabled(true);
            presentation.setTextValue(myProjectText);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        //Force move focus to IdeFrame
        IdeEventQueueProxy.getInstance().closeAllPopups();

        final int modifiers = e.getModifiers();
        final boolean forceOpenInNewFrame = BitUtil.isSet(modifiers, InputEvent.CTRL_MASK)
            || BitUtil.isSet(modifiers, InputEvent.SHIFT_MASK)
            || ActionPlaces.WELCOME_SCREEN.equals(e.getPlace());

        Project project = e.getData(Project.KEY);
        if (!new File(myProjectPath).exists()) {
            int result = Messages.showDialog(project,
                "The path " + FileUtil.toSystemDependentName(myProjectPath) + " does not exist.\n" +
                    "If it is on a removable or network drive, please make sure that the drive is connected.",
                "Reopen Project",
                new String[]{"OK", "&Remove From List"},
                0,
                UIUtil.getErrorIcon()
            );
            if (result == 1) {
                myIsRemoved = true;
                RecentProjectsManager.getInstance().removePath(myProjectPath);
            }
            return;
        }

        ProjectOpenContext context = new ProjectOpenContext();
        if (myFrameState != null) {
            context.putUserData(IdeFrameState.KEY, myFrameState);
        } else {
            IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(project);
            if (ideFrame != null) {
                IdeFrameState frameState = ideFrame.getFrameState();
                context.putUserData(IdeFrameState.KEY, frameState);
            }
        }

        ProjectImplUtil.openAsync(myProjectPath, project, forceOpenInNewFrame, UIAccess.current(), context);
    }

    public boolean isRemoved() {
        return myIsRemoved;
    }

    @Nonnull
    public Image getExtensionIcon() {
        List<String> extensions = getExtensions();
        Image moduleMainIcon = Image.empty(Image.DEFAULT_ICON_SIZE);
        if (!extensions.isEmpty()) {
            for (String extensionId : extensions) {
                ModuleExtensionProvider provider = ModuleExtensionProvider.findProvider(extensionId);
                if (provider != null) {
                    moduleMainIcon = provider.getIcon();
                    break;
                }
            }
        }
        return moduleMainIcon;
    }

    @Nonnull
    public List<String> getExtensions() {
        return myExtensions;
    }

    public String getProjectPath() {
        return myProjectPath;
    }

    public String getProjectName() {
        return myProjectName;
    }
}
