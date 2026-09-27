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
package consulo.application.util;

import consulo.localize.Localized;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

/**
 * @author UNV
 * @since 2026-09-25
 */
public class LocalizedAssert<T extends Localized> extends AbstractAssert<LocalizedAssert<T>, T> {
    LocalizedAssert(T actual) {
        super(actual, LocalizedAssert.class);
    }

    public LocalizedAssert<T> hasId(String expectedId) {
        Assertions.assertThat(actual.getId())
            .as("getId() of %s", actual.getClass().getSimpleName())
            .isEqualTo(expectedId);
        return this;
    }

    public LocalizedAssert<T> hasIdAndToString(String expectedId) {
        return hasId(expectedId).hasToString(expectedId);
    }
}
