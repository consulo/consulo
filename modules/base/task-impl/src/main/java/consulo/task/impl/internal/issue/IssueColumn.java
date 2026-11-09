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
package consulo.task.impl.internal.issue;

import consulo.application.util.DateFormatUtil;
import consulo.localize.LocalizeValue;
import consulo.task.Task;
import consulo.task.TaskState;
import consulo.task.localize.TaskLocalize;
import consulo.util.lang.StringUtil;

import java.util.Date;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
enum IssueColumn {
    ID(TaskLocalize::issuesColumnId, 90, true, Task::getPresentableId),
    SUMMARY(TaskLocalize::issuesColumnSummary, 250, true, Task::getSummary),
    TYPE(TaskLocalize::issuesColumnType, 80, true, IssueColumn::getTypeText),
    STATUS(TaskLocalize::issuesColumnState, 90, true, IssueColumn::getStateText),
    PRIORITY(TaskLocalize::issuesColumnPriority, 80, true, task -> StringUtil.notNullize(task.getPriority())),
    ASSIGNEE(TaskLocalize::issuesColumnAssignee, 90, true, task -> StringUtil.notNullize(task.getAssignee())),
    UPDATED(TaskLocalize::issuesColumnUpdated, 90, true, task -> formatDate(task.getUpdated())),
    CREATED(TaskLocalize::issuesColumnCreated, 90, false, task -> formatDate(task.getCreated()));

    private final Supplier<LocalizeValue> myHeader;
    private final int myWidth;
    private final boolean myVisibleByDefault;
    private final Function<Task, String> myValue;

    IssueColumn(Supplier<LocalizeValue> header, int width, boolean visibleByDefault, Function<Task, String> value) {
        myHeader = header;
        myWidth = width;
        myVisibleByDefault = visibleByDefault;
        myValue = value;
    }

    LocalizeValue getHeader() {
        return myHeader.get();
    }

    int getWidth() {
        return myWidth;
    }

    boolean isVisibleByDefault() {
        return myVisibleByDefault;
    }

    String getValue(Task task) {
        return myValue.apply(task);
    }

    static String getStateText(Task task) {
        TaskState state = task.getState();
        if (state != null) {
            return state.getPresentableName();
        }
        return task.isClosed() ? TaskState.RESOLVED.getPresentableName() : "";
    }

    static String getTypeText(Task task) {
        return StringUtil.capitalize(task.getType().name().toLowerCase(Locale.ROOT));
    }

    private static String formatDate(Date date) {
        return date == null ? "" : DateFormatUtil.formatPrettyDate(date);
    }
}
