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
package consulo.execution.test.sm;

import consulo.component.util.localize.AbstractBundle;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author VISTALL
 * @since 2024-03-02
 */
public class SmRunnerBundle extends AbstractBundle {
    private static final String BUNDLE = "consulo.execution.test.sm.SmRunnerBundle";

    private static final SmRunnerBundle INSTANCE = new SmRunnerBundle();

    private SmRunnerBundle() {
        super(BUNDLE);
    }

    @Nonnull
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String messageKey, Object... params) {
        return INSTANCE.getMessage(messageKey, params);
    }
}
