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
package consulo.it.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.localHistory.ActivityId;
import consulo.localHistory.FileRevisionTimestampComparator;
import consulo.localHistory.Label;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * Keeps no history: actions and labels are no-ops.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ServiceImpl
@Singleton
public class HeadlessLocalHistory extends LocalHistory {
    @Override
    public LocalHistoryAction startAction(LocalizeValue name, @Nullable ActivityId activityId) {
        return LocalHistoryAction.NULL;
    }

    @Override
    public Label putSystemLabel(Project project, LocalizeValue name, int color) {
        return Label.NULL_INSTANCE;
    }

    @Override
    public Label putUserLabel(Project project, LocalizeValue name) {
        return Label.NULL_INSTANCE;
    }

    @Override
    public byte @Nullable [] getByteContent(VirtualFile f, FileRevisionTimestampComparator c) {
        return null;
    }

    @Override
    public boolean isUnderControl(VirtualFile f) {
        return false;
    }

    @Override
    public void cleanupForNextTest() {
    }
}
