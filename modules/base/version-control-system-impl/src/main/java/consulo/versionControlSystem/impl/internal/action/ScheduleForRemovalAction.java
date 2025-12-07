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
package consulo.versionControlSystem.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.impl.internal.change.action.AbstractMissingFilesAction;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * @author yole
 * @since 2006-11-02
 */
@ActionImpl(id = "ChangesView.RemoveDeleted")
public class ScheduleForRemovalAction extends AbstractMissingFilesAction {
    public ScheduleForRemovalAction() {
        super(
            VcsLocalize.actionChangesViewRemoveDeletedText(),
            VcsLocalize.actionChangesViewRemoveDeletedDescription(),
            PlatformIconGroup.actionsCancel()
        );
    }

    @Override
    protected List<VcsException> processFiles(AbstractVcs vcs, List<FilePath> files) {
        CheckinEnvironment environment = vcs.getCheckinEnvironment();
        if (environment == null) {
            return Collections.emptyList();
        }
        List<VcsException> result = environment.scheduleMissingFileForDeletion(files);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    @Nonnull
    @Override
    protected LocalizeValue getName() {
        return LocalizeValue.absent();
    }

    @Override
    protected boolean synchronously() {
        return true;
    }
}