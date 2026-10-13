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
package consulo.fileChooser.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import java.util.List;

/**
 * Tabs for other machines - one per remote platform. They are offered only where the chooser answers paths and the
 * descriptor {@link consulo.fileChooser.FileChooserDescriptor#isAnyPlatform() allows any platform}, since a remote file
 * has no virtual file.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PlatformFileChooserContributorProvider {
    List<UniversalFileChooserContributor> getContributors();
}
