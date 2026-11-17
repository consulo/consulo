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

import consulo.annotation.component.ActionImpl;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.frame.HasThreadInterrupt;
import consulo.execution.debug.frame.HasThreadSuspend;
import consulo.execution.debug.localize.XDebuggerLocalize;

/**
 * @author VISTALL
 * @since 2026-11-17
 */
@ActionImpl(id = XDebuggerActions.INTERRUPT_THREAD)
public class InterruptThreadAction extends ThreadActionBase<HasThreadInterrupt> {
    public InterruptThreadAction() {
        super(XDebuggerLocalize.actionInterruptThreadText(), HasThreadInterrupt.class);
    }

    /**
     * A frozen thread is not interrupted - it would not see the interruption until it is resumed.
     */
    @Override
    protected boolean isApplicable(HasThreadInterrupt thread) {
        return !(thread instanceof HasThreadSuspend hasThreadSuspend && hasThreadSuspend.isSuspended());
    }

    @Override
    protected void perform(HasThreadInterrupt thread) {
        thread.interruptThread();
    }
}
