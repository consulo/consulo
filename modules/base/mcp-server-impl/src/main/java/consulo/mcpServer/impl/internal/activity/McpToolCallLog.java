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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpToolCallLog {
    private static final int MAX_RECORDS = 500;
    private static final int MAX_TEXT = 4000;

    private final Application myApplication;
    private final Deque<McpToolCallRecord> myRecords = new ArrayDeque<>();

    @Inject
    public McpToolCallLog(Application application) {
        myApplication = application;
    }

    public McpToolCallRecord start(String clientName, @Nullable String projectName, String toolName, String arguments) {
        McpToolCallRecord record = new McpToolCallRecord(System.currentTimeMillis(),
                                                         clientName,
                                                         projectName,
                                                         toolName,
                                                         StringUtil.first(arguments, MAX_TEXT, true));
        synchronized (myRecords) {
            myRecords.addLast(record);
            while (myRecords.size() > MAX_RECORDS) {
                myRecords.removeFirst();
            }
        }
        myApplication.getMessageBus().syncPublisher(McpToolCallListener.class).callStarted(record);
        return record;
    }

    public void changeStatus(McpToolCallRecord record, McpToolCallStatus status) {
        record.setStatus(status);
        myApplication.getMessageBus().syncPublisher(McpToolCallListener.class).callChanged(record);
    }

    public void finish(McpToolCallRecord record, McpToolCallStatus status, String result) {
        record.finish(status, StringUtil.first(result, MAX_TEXT, true), System.currentTimeMillis());
        myApplication.getMessageBus().syncPublisher(McpToolCallListener.class).callChanged(record);
    }

    public List<McpToolCallRecord> getRecords() {
        synchronized (myRecords) {
            return new ArrayList<>(myRecords);
        }
    }
}
