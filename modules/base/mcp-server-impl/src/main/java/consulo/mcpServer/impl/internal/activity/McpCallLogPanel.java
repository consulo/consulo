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
package consulo.mcpServer.impl.internal.activity;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.mcpServer.localize.McpServerLocalize;
import consulo.util.lang.StringUtil;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.Table;
import consulo.ui.TextArea;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
public final class McpCallLogPanel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MutableFlatDataModel<McpToolCallRecord> myModel;
    private final Table<McpToolCallRecord> myTable;
    private final ScrollableLayout myLayout;
    private @Nullable McpToolCallRecord myExpanded;

    @RequiredUIAccess
    public McpCallLogPanel(Application application, McpToolCallLog callLog, Disposable parentDisposable) {
        List<McpToolCallRecord> records = new ArrayList<>(callLog.getRecords());
        Collections.reverse(records);
        myModel = FlatDataModel.of(records);

        // a narrow tab shows what was called and how it went - the rest opens under the row of the call
        myTable = Table.create(myModel);
        myTable.addColumn(McpServerLocalize.columnTime(), record -> formatTime(record.getStartedAt())).setWidth(70);
        myTable.addColumn(McpServerLocalize.columnTool(), McpToolCallRecord::getToolName).setWidth(180);
        myTable.addColumn(McpServerLocalize.columnStatus(), record -> statusText(record.getStatus()).get()).setWidth(120);
        myTable.setDetailsRenderer(McpCallLogPanel::details);
        myTable.addSelectListener(event -> expand(event.getValue()));

        myLayout = ScrollableLayout.create(myTable);

        UIAccess uiAccess = UIAccess.current();
        application.getMessageBus().connect(parentDisposable).subscribe(McpToolCallListener.class, new McpToolCallListener() {
            @Override
            public void callStarted(McpToolCallRecord record) {
                uiAccess.execute(() -> myModel.add(record, 0));
            }

            @Override
            public void callChanged(McpToolCallRecord record) {
                uiAccess.execute(() -> {
                    myModel.update(record);
                    // the details show what the call is now - its result once it has one
                    if (myExpanded == record) {
                        myTable.setDetailsVisible(record, false);
                        myTable.setDetailsVisible(record, true);
                    }
                });
            }
        });
    }

    /**
     * One call at a time shows its details - the one selected last.
     */
    @RequiredUIAccess
    private void expand(@Nullable McpToolCallRecord record) {
        McpToolCallRecord expanded = myExpanded;
        if (expanded == record) {
            return;
        }
        if (expanded != null) {
            myTable.setDetailsVisible(expanded, false);
        }
        myExpanded = record;
        if (record != null) {
            myTable.setDetailsVisible(record, true);
        }
    }

    @RequiredUIAccess
    private static Component details(McpToolCallRecord record) {
        VerticalLayout details = VerticalLayout.create(Space.X_SMALL);
        details.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        details.paddingBuilder().allSet(Space.SMALL).apply();
        details.add(row(McpServerLocalize.columnClient(), record.getClientName()));
        String project = record.getProjectName();
        if (project != null) {
            details.add(row(McpServerLocalize.columnProject(), project));
        }
        String duration = durationText(record);
        if (!duration.isEmpty()) {
            details.add(row(McpServerLocalize.columnDuration(), duration));
        }
        details.add(text(McpServerLocalize.labelCallArguments(), record.getArguments()));
        if (!record.getResult().isEmpty()) {
            details.add(text(McpServerLocalize.labelCallResult(), record.getResult()));
        }
        return details;
    }

    @RequiredUIAccess
    private static Component row(LocalizeValue name, String value) {
        Label label = Label.create(name);
        label.addStyle(LabelStyle.FONT_SMALL);
        return DockLayout.create().left(label).right(Label.create(LocalizeValue.of(StringUtil.escapeMnemonics(value))));
    }

    @RequiredUIAccess
    private static Component text(LocalizeValue name, String value) {
        VerticalLayout layout = VerticalLayout.create(Space.X_SMALL);
        layout.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        Label label = Label.create(name);
        label.addStyle(LabelStyle.FONT_SMALL);
        layout.add(label);
        TextArea area = TextArea.create(value);
        area.setEditable(false);
        layout.add(area);
        return layout;
    }

    public Component getComponent() {
        return myLayout;
    }

    private static String formatTime(long millis) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    private static String durationText(McpToolCallRecord record) {
        long finishedAt = record.getFinishedAt();
        return finishedAt == 0 ? "" : (finishedAt - record.getStartedAt()) + " ms";
    }

    private static LocalizeValue statusText(McpToolCallStatus status) {
        return switch (status) {
            case WAITING_FOR_APPROVAL -> McpServerLocalize.statusWaitingForApproval();
            case RUNNING -> McpServerLocalize.statusRunning();
            case SUCCEEDED -> McpServerLocalize.statusSucceeded();
            case FAILED -> McpServerLocalize.statusFailed();
            case DENIED -> McpServerLocalize.statusDenied();
        };
    }
}
