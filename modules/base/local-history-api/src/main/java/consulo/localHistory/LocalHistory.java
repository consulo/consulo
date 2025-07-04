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
package consulo.localHistory;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
public abstract class LocalHistory {
    public static final Object VFS_EVENT_REQUESTOR = new Object();

    public static LocalHistory getInstance() {
        return Application.get().getInstance(LocalHistory.class);
    }

    public LocalHistoryAction startAction(@Nonnull LocalizeValue name) {
        return startAction(name.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public abstract LocalHistoryAction startAction(@Nullable String name);

    public Label putSystemLabel(Project project, @Nonnull LocalizeValue name, int color) {
        return putSystemLabel(project, name.get(), color);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public abstract Label putSystemLabel(Project project, @Nonnull String name, int color);

    public Label putSystemLabel(Project project, @Nonnull LocalizeValue name) {
        return putSystemLabel(project, name, -1);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public Label putSystemLabel(Project project, @Nonnull String name) {
        return putSystemLabel(project, name, -1);
    }

    public Label putUserLabel(Project project, @Nonnull LocalizeValue name) {
        return putUserLabel(project, name.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public abstract Label putUserLabel(Project p, @Nonnull String name);

    @Nullable
    public abstract byte[] getByteContent(VirtualFile f, FileRevisionTimestampComparator c);

    public abstract boolean isUnderControl(VirtualFile f);

    @TestOnly
    public abstract void cleanupForNextTest();
}
