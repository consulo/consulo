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
package consulo.sandboxPlugin.ide.review;

import consulo.versionControlSystem.history.ShortVcsRevisionNumber;
import consulo.versionControlSystem.history.VcsRevisionNumber;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public record SandRevisionNumber(String sha) implements ShortVcsRevisionNumber {
    @Override
    public String asString() {
        return sha;
    }

    @Override
    public String toShortString() {
        return sha.length() > 8 ? sha.substring(0, 8) : sha;
    }

    @Override
    public int compareTo(VcsRevisionNumber other) {
        return sha.compareTo(other.asString());
    }
}
