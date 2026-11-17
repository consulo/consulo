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
package consulo.execution.debug.impl.internal.action.thread;

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;

import java.util.ArrayList;
import java.util.List;

/**
 * An action on the threads selected in the Threads view, applicable when all of them implement {@code T}.
 *
 * @author VISTALL
 * @since 2026-11-17
 */
public abstract class ThreadActionBase<T> extends AnAction implements AnActionWithSyncUpdate {
    private final Class<T> myCapability;

    protected ThreadActionBase(LocalizeValue text, Class<T> capability) {
        super(text);
        myCapability = capability;
    }

    protected abstract boolean isApplicable(T thread);

    protected abstract void perform(T thread);

    @Override
    public void update(AnActionEvent e) {
        List<T> threads = getSelectedThreads(e);
        e.getPresentation().setEnabledAndVisible(!threads.isEmpty() && threads.stream().allMatch(this::isApplicable));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        List<T> threads = getSelectedThreads(e);
        for (T thread : threads) {
            if (isApplicable(thread)) {
                perform(thread);
            }
        }

        XDebugSession session = e.getData(XDebugSession.DATA_KEY);
        if (session != null) {
            session.rebuildViews();
        }
    }

    /**
     * The selected threads, empty when one of them does not implement {@code T}.
     */
    private List<T> getSelectedThreads(AnActionEvent e) {
        List<XExecutionStack> stacks = e.getData(XExecutionStack.SELECTED_STACKS);
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }
        List<T> threads = new ArrayList<>(stacks.size());
        for (XExecutionStack stack : stacks) {
            if (!myCapability.isInstance(stack)) {
                return List.of();
            }
            threads.add(myCapability.cast(stack));
        }
        return threads;
    }
}
