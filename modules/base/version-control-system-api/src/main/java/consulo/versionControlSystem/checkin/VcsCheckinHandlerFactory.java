/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.checkin;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.CommitContext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author irengrig
 * @since 2011-01-28
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class VcsCheckinHandlerFactory implements BaseCheckinHandlerFactory {
    public static final ExtensionPointName<VcsCheckinHandlerFactory> EP_NAME = ExtensionPointName.create(VcsCheckinHandlerFactory.class);

    private VcsKey myKey;

    protected VcsCheckinHandlerFactory(@Nonnull VcsKey key) {
        myKey = key;
    }

    @Nullable
    @Override
    public CheckinHandler createHandler(CheckinProjectPanel panel, CommitContext commitContext) {
        if (!panel.vcsIsAffected(myKey.getName())) {
            return null;
        }
        return createVcsHandler(panel);
    }

    @Nonnull
    protected abstract CheckinHandler createVcsHandler(CheckinProjectPanel panel);

    public VcsKey getKey() {
        return myKey;
    }

    @Override
    public BeforeCheckinDialogHandler createSystemReadyHandler(Project project) {
        return null;
    }
}
