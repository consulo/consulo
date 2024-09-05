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
package consulo.logger.internal.impl.logback;

import consulo.logging.internal.LoggerFactory;
import consulo.logging.internal.LoggerFactoryProvider;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-09-05
 */
public class LogbackLoggerFactoryProvider implements LoggerFactoryProvider {
    public LogbackLoggerFactoryProvider() {
    }

    @Override
    public int getPriority() {
        return HIGHT_PRIORITY;
    }

    @Nonnull
    @Override
    public LoggerFactory create() {
        return new LogbackLoggerFactory();
    }
}
