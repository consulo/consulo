/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.impl.internal.dataRule;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.versionControlSystem.VcsDataKeys;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class VcsUiDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(@Nonnull DataSink sink, @Nonnull DataSnapshot snapshot) {
        sink.lazyValue(VcsDataKeys.CHANGES_SELECTION, VcsChangesSelectionRule::getChangesSelection);
        sink.lazyValue(VcsDataKeys.VCS_REVISION_NUMBERS, VcsRevisionNumberArrayRule::getData);
        sink.lazyValue(VcsDataKeys.VIRTUAL_FILE_STREAM, VirtualFileStreamRule::getData);
    }
}
