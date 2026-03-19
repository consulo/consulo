/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.localHistory.impl.internal.ui.action;

import consulo.application.dumb.DumbAware;
import consulo.localHistory.impl.internal.IdeaGateway;
import consulo.localHistory.impl.internal.LocalHistoryFacade;
import consulo.localHistory.impl.internal.LocalHistoryImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.collection.Streams;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import static consulo.util.lang.ObjectUtil.notNull;

public abstract class LocalHistoryAction extends AnAction implements DumbAware {
    protected LocalHistoryAction() {
    }

    protected LocalHistoryAction(LocalizeValue text) {
        super(text);
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation p = e.getPresentation();

        if (!e.hasData(Project.KEY)) {
            p.setEnabledAndVisible(false);
        }
        else {
            p.setVisible(true);
            p.setTextValue(getTextValue(e));

            LocalHistoryFacade vcs = getVcs();
            IdeaGateway gateway = getGateway();
            p.setEnabled(vcs != null && gateway != null && isEnabled(vcs, gateway, e));
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        actionPerformed(e.getRequiredData(Project.KEY), notNull(getGateway()), e);
    }

    
    protected LocalizeValue getTextValue(AnActionEvent e) {
        return e.getPresentation().getTextValue();
    }

    protected boolean isEnabled(LocalHistoryFacade vcs, IdeaGateway gw, AnActionEvent e) {
        return isEnabled(vcs, gw, getFile(e), e);
    }

    protected void actionPerformed(Project p, IdeaGateway gw, AnActionEvent e) {
        actionPerformed(p, gw, notNull(getFile(e)), e);
    }

    protected boolean isEnabled(
        LocalHistoryFacade vcs,
        IdeaGateway gw,
        @Nullable VirtualFile f,
        AnActionEvent e
    ) {
        return true;
    }

    protected void actionPerformed(Project p, IdeaGateway gw, VirtualFile f, AnActionEvent e) {
    }

    protected @Nullable LocalHistoryFacade getVcs() {
        return LocalHistoryImpl.getInstanceImpl().getFacade();
    }

    protected @Nullable IdeaGateway getGateway() {
        return LocalHistoryImpl.getInstanceImpl().getGateway();
    }

    protected @Nullable VirtualFile getFile(AnActionEvent e) {
        return Streams.getIfSingle(e.getData(VcsDataKeys.VIRTUAL_FILE_STREAM));
    }
}
