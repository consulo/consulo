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
import consulo.ui.Component;
import consulo.ui.Table;
import consulo.ui.TextArea;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
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
    private final TextArea myDetails;
    private final TwoComponentSplitLayout myLayout;

    @RequiredUIAccess
    public McpCallLogPanel(Application application, McpToolCallLog callLog, Disposable parentDisposable) {
        List<McpToolCallRecord> records = new ArrayList<>(callLog.getRecords());
        Collections.reverse(records);
        myModel = FlatDataModel.of(records);

        myTable = Table.create(myModel);
        myTable.addColumn(McpServerLocalize.columnTime(), record -> formatTime(record.getStartedAt())).setWidth(80);
        myTable.addColumn(McpServerLocalize.columnClient(), McpToolCallRecord::getClientName).setWidth(160);
        myTable.addColumn(McpServerLocalize.columnProject(), record -> record.getProjectName() == null ? "" : record.getProjectName()).setWidth(140);
        myTable.addColumn(McpServerLocalize.columnTool(), McpToolCallRecord::getToolName).setWidth(200);
        myTable.addColumn(McpServerLocalize.columnStatus(), record -> statusText(record.getStatus()).get()).setWidth(150);
        myTable.addColumn(McpServerLocalize.columnDuration(), McpCallLogPanel::durationText).setWidth(90);
        myTable.addSelectListener(event -> showDetails(event.getValue()));

        myDetails = TextArea.create();
        myDetails.setEditable(false);

        myLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
        myLayout.setFirstComponent(ScrollableLayout.create(myTable));
        myLayout.setSecondComponent(myDetails);
        myLayout.setProportion(70);

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
                    if (myTable.getSelectedItem() == record) {
                        showDetails(record);
                    }
                });
            }
        });
    }

    public Component getComponent() {
        return myLayout;
    }

    @RequiredUIAccess
    private void showDetails(@Nullable McpToolCallRecord record) {
        if (record == null) {
            myDetails.setValue("");
            return;
        }
        myDetails.setValue(record.getToolName() + " " + record.getArguments() + "\n\n" + record.getResult());
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
