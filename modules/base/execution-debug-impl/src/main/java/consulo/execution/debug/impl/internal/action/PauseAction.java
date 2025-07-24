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
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.execution.debug.impl.internal.action.handler.DebuggerActionHandler;
import consulo.execution.debug.impl.internal.action.handler.XDebuggerPauseActionHandler;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ActionImpl(id = "Pause")
public class PauseAction extends XDebuggerActionBase {
    private final XDebuggerPauseActionHandler myHandler = new XDebuggerPauseActionHandler();

    public PauseAction() {
        super(
            XDebuggerLocalize.actionPauseText(),
            XDebuggerLocalize.actionPauseDescription(),
            PlatformIconGroup.actionsPause()
        );
    }

    @Nonnull
    @Override
    protected DebuggerActionHandler getHandler() {
        return myHandler;
    }
}
