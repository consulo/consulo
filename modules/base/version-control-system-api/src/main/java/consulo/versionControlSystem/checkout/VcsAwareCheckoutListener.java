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
package consulo.versionControlSystem.checkout;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.versionControlSystem.VcsKey;
import consulo.project.Project;

import java.io.File;

/**
 * @author Irina.Chernushina
 * @since 2011-11-19
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface VcsAwareCheckoutListener {
    ExtensionPointName<VcsAwareCheckoutListener> EP_NAME = ExtensionPointName.create(VcsAwareCheckoutListener.class);

    boolean processCheckedOutDirectory(final Project project, final File directory, final VcsKey vcsKey);
}
