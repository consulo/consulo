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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Decides whether a value being entered is acceptable. One method produces both the verdict and the
 * text to show, so the two can never disagree.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
@FunctionalInterface
public interface InputValidator<V> {
    /**
     * Runs on every change.
     *
     * @return null when the value is fine
     */
    @RequiredUIAccess
    @Nullable
    InputProblem validate(V value);

    /**
     * Runs once, when the box is confirmed and {@link #validate} has passed. The box disables itself
     * until the returned result arrives; a problem keeps it open with the entered value intact.
     * Work which can fail belongs here rather than after the box has closed.
     */
    @RequiredUIAccess
    default CompletableFuture<@Nullable InputProblem> confirm(V value) {
        return CompletableFuture.completedFuture(null);
    }

    default InputValidator<V> and(InputValidator<V> next) {
        return value -> {
            InputProblem problem = validate(value);
            return problem != null ? problem : next.validate(value);
        };
    }
}
