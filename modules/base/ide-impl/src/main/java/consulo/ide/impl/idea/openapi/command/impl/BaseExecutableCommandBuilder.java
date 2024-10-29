/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.openapi.command.impl;

import consulo.undoRedo.builder.ExecutableCommandBuilder;
import consulo.util.lang.function.ThrowableSupplier;

/**
 * @author UNV
 * @since 2024-10-28
 */
public class BaseExecutableCommandBuilder<R, THIS extends ExecutableCommandBuilder<R, THIS>>
    extends BaseCommandBuilder<THIS>  implements ExecutableCommandBuilder<R, THIS> {

    protected boolean myGlobalUndoAction = false;

    @Override
    public THIS inGlobalUndoAction() {
        myGlobalUndoAction = true;
        return self();
    }

    @Override
    public ExecutionResult<R> execute(ThrowableSupplier<R, ? extends Throwable> executable) {
        return ExecutionResult.execute(executable);
    }
}
