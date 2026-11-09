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
package consulo.continuousIntegration;

import java.nio.file.Path;

/**
 * Value of a parameter when a job is started.
 *
 * @since 2026-11-09
 */
public sealed interface CIParameterValue {
    static CIParameterValue text(String value) {
        return new Text(value);
    }

    static CIParameterValue file(Path file) {
        return new File(file);
    }

    record Text(String value) implements CIParameterValue {
    }

    record File(Path file) implements CIParameterValue {
    }
}
