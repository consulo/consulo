/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.action.handler;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.breakpoint.*;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroupingRule;
import consulo.execution.debug.event.XBreakpointListener;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointItem;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointPanelProvider;
import consulo.execution.debug.impl.internal.breakpoint.ui.group.XBreakpointCustomGroupingRule;
import consulo.execution.debug.impl.internal.breakpoint.ui.group.XBreakpointFileGroupingRule;
import consulo.execution.debug.impl.internal.breakpoint.ui.group.XBreakpointGroupingByTypeRule;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.Lists;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class XBreakpointPanelProvider extends BreakpointPanelProvider<XBreakpoint> {
    @Nonnull
    public static XBreakpointPanelProvider getInstance() {
        return Application.get().getInstance(XBreakpointPanelProvider.class);
    }

    private final List<MyXBreakpointListener> myListeners = Lists.newLockFreeCopyOnWriteList();

    @Override
    public void createBreakpointsGroupingRules(Collection<XBreakpointGroupingRule> rules) {
        rules.add(new XBreakpointGroupingByTypeRule());
        rules.add(new XBreakpointFileGroupingRule());
        rules.add(new XBreakpointCustomGroupingRule());
    }

    @Override
    public void addListener(BreakpointsListener listener, Project project, Disposable disposable) {
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        MyXBreakpointListener listener1 = new MyXBreakpointListener(listener, breakpointManager);
        breakpointManager.addBreakpointListener(listener1);
        myListeners.add(listener1);
        Disposer.register(disposable, () -> removeListener(listener));
    }

    @Override
    protected void removeListener(BreakpointsListener listener) {
        for (MyXBreakpointListener breakpointListener : myListeners) {
            if (breakpointListener.myListener == listener) {
                XBreakpointManager manager = breakpointListener.myBreakpointManager;
                manager.removeBreakpointListener(breakpointListener);
                myListeners.remove(breakpointListener);
                break;
            }
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    @Nullable
    public XBreakpoint<?> findBreakpoint(@Nonnull Project project, @Nonnull Document document, int offset) {
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        int line = document.getLineNumber(offset);
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return null;
        }
        for (XLineBreakpointType<?> type : XDebuggerUtil.getInstance().getLineBreakpointTypes()) {
            XLineBreakpoint<? extends XBreakpointProperties> breakpoint = breakpointManager.findBreakpointAtLine(type, file, line);
            if (breakpoint != null) {
                return breakpoint;
            }
        }

        return null;
    }

    @Override
    public GutterIconRenderer getBreakpointGutterIconRenderer(Object breakpoint) {
        if (breakpoint instanceof XLineBreakpoint lineBreakpoint) {
            RangeHighlighter highlighter = lineBreakpoint.getHighlighter();
            if (highlighter != null) {
                return highlighter.getGutterIconRenderer();
            }
        }
        return null;
    }

    @Override
    public void onDialogClosed(Project project) {
    }

    @Override
    public void provideBreakpointItems(Project project, Collection<BreakpointItem> items) {
        List<XBreakpointType> types = XBreakpointUtil.getBreakpointTypes();
        XBreakpointManager manager = XDebuggerManager.getInstance(project).getBreakpointManager();
        for (XBreakpointType<?, ?> type : types) {
            Collection<? extends XBreakpoint<?>> breakpoints = manager.getBreakpoints(type);
            if (breakpoints.isEmpty()) {
                continue;
            }
            for (XBreakpoint<?> breakpoint : breakpoints) {
                items.add(new XBreakpointItem(breakpoint));
            }
        }
    }

    private static class MyXBreakpointListener implements XBreakpointListener<XBreakpoint<?>> {
        public BreakpointsListener myListener;
        public XBreakpointManager myBreakpointManager;

        public MyXBreakpointListener(BreakpointsListener listener, XBreakpointManager breakpointManager) {
            myListener = listener;
            myBreakpointManager = breakpointManager;
        }

        @Override
        public void breakpointAdded(@Nonnull XBreakpoint<?> breakpoint) {
            myListener.breakpointsChanged();
        }

        @Override
        public void breakpointRemoved(@Nonnull XBreakpoint<?> breakpoint) {
            myListener.breakpointsChanged();
        }

        @Override
        public void breakpointChanged(@Nonnull XBreakpoint<?> breakpoint) {
            myListener.breakpointsChanged();
        }
    }

    private static class AddXBreakpointAction extends AnAction {

        private final XBreakpointType<?, ?> myType;

        public AddXBreakpointAction(XBreakpointType<?, ?> type) {
            myType = type;
            getTemplatePresentation().setIcon(type.getEnabledIcon());
            getTemplatePresentation().setText(type.getTitle());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            myType.addBreakpoint(e.getData(Project.KEY), null);
        }
    }
}
