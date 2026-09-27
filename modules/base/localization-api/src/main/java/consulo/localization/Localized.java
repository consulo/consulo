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
package consulo.localization;

/**
 * Object which toString() produces localized result which depends on current locale.
 *
 * @author UNV
 * @since 2026-09-25
 */
public interface Localized {
    /**
     * Tells if this {@link Localized} object resolves to empty {@code String} regardless of locale.
     *
     * @return {@code true} if this object resolves to empty {@code String} regardless of locale.
     */
    default boolean isEmpty() {
        return false;
    }

    /**
     * String which doesn't depend on current locale. Two equal {@link Localized} objects must have equal ids.
     * Different ones should have different ids (though 100% collision-free may be not guaranteed).
     *
     * @return String uniquely identifying this {@link Localized} object.
     */
    String getId();

    /**
     * Produces localized result which depends on current locale.
     *
     * @return Localized text.
     */
    @Override
    String toString();
}
