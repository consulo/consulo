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

package consulo.versionControlSystem.checkin;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.CommitContext;

import jakarta.annotation.Nullable;

/**
 * Factory which provides callbacks to run before and after checkin operations.
 * !! This factory is loaded on first commit
 * should NOT be used from VCS plugins
 * use {@link VcsCheckinHandlerFactory} implementations instead
 * they would automatically would be registered in {@link AbstractVcs#activate()}
 * and unregistered in {@link AbstractVcs#deactivate()}
 *
 * @author lesya
 * @since 5.1
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CheckinHandlerFactory implements BaseCheckinHandlerFactory {
    public static final ExtensionPointName<CheckinHandlerFactory> EP_NAME = ExtensionPointName.create(CheckinHandlerFactory.class);

    /**
     * Creates a handler for a single Checkin Project or Checkin File operation.
     *
     * @param panel         the class which can be used to retrieve information about the files to be committed,
     *                      and to get or set the commit message.
     * @param commitContext Context of the commit.
     * @return the handler instance.
     */
    @Override
    @Nullable
    public abstract CheckinHandler createHandler(CheckinProjectPanel panel, CommitContext commitContext);

    @Override
    public BeforeCheckinDialogHandler createSystemReadyHandler(Project project) {
        return null;
    }
}
