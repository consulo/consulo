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
package consulo.sandboxPlugin.tasks;

import consulo.task.Comment;
import consulo.task.Task;
import consulo.task.TaskType;
import consulo.task.icon.TaskIconGroup;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Date;

/**
 * @author VISTALL
 * @since 2025-08-21
 */
public class SandTask extends Task {
    private String myId;
    private String mySummary;
    private boolean myClosed;

    public SandTask() {
    }

    public SandTask(String id, String summary, boolean closed) {
        myId = id;
        mySummary = summary;
        myClosed = closed;
    }

    @Nonnull
    @Override
    public String getId() {
        return myId;
    }

    @Nonnull
    @Override
    public String getSummary() {
        return mySummary;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nonnull
    @Override
    public Comment[] getComments() {
        return new Comment[0];
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return TaskIconGroup.bug();
    }

    @Nonnull
    @Override
    public TaskType getType() {
        return TaskType.BUG;
    }

    @Nullable
    @Override
    public Date getUpdated() {
        return null;
    }

    @Nullable
    @Override
    public Date getCreated() {
        return null;
    }

    @Override
    public boolean isClosed() {
        return myClosed;
    }

    @Override
    public boolean isIssue() {
        return true;
    }

    @Nullable
    @Override
    public String getIssueUrl() {
        return null;
    }
}
