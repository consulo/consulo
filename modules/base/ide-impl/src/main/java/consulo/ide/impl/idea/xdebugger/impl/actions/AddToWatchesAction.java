/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.xdebugger.impl.actions;

import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.ide.impl.idea.xdebugger.impl.DebuggerSupport;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class AddToWatchesAction extends XDebuggerActionBase {
    public AddToWatchesAction() {
        super(true);
    }

    @Nonnull
    @Override
    protected DebuggerActionHandler getHandler(@Nonnull DebuggerSupport debuggerSupport) {
        return debuggerSupport.getAddToWatchesActionHandler();
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return ExecutionDebugIconGroup.actionAddtowatch();
    }
}
