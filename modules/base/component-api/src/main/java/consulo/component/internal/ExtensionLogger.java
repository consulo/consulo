/*
 * Copyright 2013-2025 consulo.io
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
package consulo.component.internal;

import consulo.component.extension.ExtensionPoint;
import consulo.component.util.PluginExceptionUtil;
import consulo.logging.Logger;
import consulo.util.lang.ControlFlowException;

/**
 * @author VISTALL
 * @since 2025-04-19
 */
public class ExtensionLogger {
    public static void checkException(Throwable e, Object value) {
        if (e instanceof ControlFlowException) {
            throw ControlFlowException.rethrow(e);
        }

        Logger logger = Logger.getInstance(ExtensionPoint.class);
        PluginExceptionUtil.logPluginError(logger, e.getMessage(), e, value != null ? value.getClass() : Void.TYPE);
    }
}
