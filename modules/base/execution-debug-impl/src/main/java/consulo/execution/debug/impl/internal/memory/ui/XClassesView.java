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
package consulo.execution.debug.impl.internal.memory.ui;

import consulo.dataContext.DataSink;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.memory.TrackerForNewInstancesBase;
import consulo.execution.debug.memory.TypeInfo;
import consulo.execution.debug.memory.XMemoryClasses;
import consulo.execution.debug.memory.XMemoryViewHandler;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The types of the debugged runtime with their instance counts, filled by the {@link XMemoryViewHandler} of the process.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
public class XClassesView extends ClassesFilteredViewBase {
    public static final Key<XClassesView> KEY = Key.create("XClassesView");

    private static final Logger LOG = Logger.getInstance(XClassesView.class);

    private final XDebugSession mySession;
    private final XMemoryViewHandler myHandler;
    private final Map<TypeInfo, Optional<TrackerForNewInstancesBase>> myTrackers = new ConcurrentHashMap<>();

    private boolean myActive;

    public XClassesView(XDebugSession session, XMemoryViewHandler handler) {
        super(session);
        mySession = session;
        myHandler = handler;
    }

    public XMemoryViewHandler getHandler() {
        return myHandler;
    }

    /**
     * The view is shown or hidden.
     */
    public void setActive(boolean active) {
        if (myActive == active) {
            return;
        }
        myActive = active;
        myHandler.setActive(active);
        if (active) {
            doActivate();
        }
        else {
            doPause();
        }
    }

    @Override
    protected void scheduleUpdateClassesCommand(XSuspendContext context) {
        myHandler.computeClasses(context).whenComplete((classes, error) -> myProject.getApplication().invokeLater(() -> {
            ClassesTable table = getTable();
            if (error != null) {
                LOG.warn(error);
            }
            else {
                updateTable(table, classes);
            }
            table.setBusy(false);
            viewUpdated();
        }));
    }

    private static void updateTable(ClassesTable table, XMemoryClasses classes) {
        Map<TypeInfo, Long> counts = classes.counts();
        if (counts != null) {
            table.updateContent(counts);
        }
        else {
            table.updateClassesOnly(classes.types());
        }
    }

    @Override
    protected @Nullable TrackerForNewInstancesBase getStrategy(TypeInfo ref) {
        return myTrackers.computeIfAbsent(ref, type -> Optional.ofNullable(myHandler.createNewInstancesTracker(type))).orElse(null);
    }

    @Override
    protected InstancesWindowBase getInstancesWindow(TypeInfo ref, XDebugSession debugSession) {
        return new XInstancesWindow(debugSession, myHandler, ref);
    }

    /**
     * Opens the instances of {@code type}, from an action.
     */
    public void showInstances(TypeInfo type) {
        handleClassSelection(type);
    }

    @Override
    public void dispose() {
        if (myActive) {
            myActive = false;
            myHandler.setActive(false);
        }
        myTrackers.clear();
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(KEY, this);
    }
}
