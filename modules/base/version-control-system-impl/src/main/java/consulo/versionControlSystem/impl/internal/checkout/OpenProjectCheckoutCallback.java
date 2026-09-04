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
package consulo.versionControlSystem.impl.internal.checkout;

import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.ui.UIAccess;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.checkout.CheckoutCallback;

import java.io.File;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 2026-09-04
 */
public class OpenProjectCheckoutCallback implements CheckoutCallback {
    private final UIAccess myUIAccess;
    private File myDirectory;
    private VcsKey myVcs;

    public OpenProjectCheckoutCallback(UIAccess UIAccess) {
        myUIAccess = UIAccess;
    }

    @Override
    public void directoryCheckedOut(File directory, VcsKey vcs) {
        myDirectory = directory;
        myVcs = vcs;
    }

    @Override
    public void checkoutCompleted() {
        ProjectManager.getInstance().openProjectAsync(myDirectory.toPath(), myUIAccess, new ProjectOpenContext())
            .whenComplete((project, throwable) -> {
                if (project == null) {
                    return;
                }

                ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
                if (!vcsManager.hasAnyMappings()) {
                    vcsManager.setDirectoryMappings(Collections.singletonList(VcsDirectoryMapping.createDefault(myVcs.getName())));
                }
            });
    }
}
