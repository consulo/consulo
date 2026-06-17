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
package consulo.sandboxPlugin.lang.moduleAware;

import java.util.Set;

/**
 * Sample module-aware options payload for Sand files. Intentionally minimal — a set of
 * preprocessor-like symbols plus a target tag. Exists to validate the end-to-end
 * module-aware indexing pipeline (record equality, deterministic serialisation,
 * provider dispatch, write-path recording, rootsChanged revalidation).
 *
 * <p>Must be a record so {@code T extends Record} on
 * {@link consulo.language.psi.stub.IndexOption#sharablePerOption} is satisfied.</p>
 */
public record SandOptions(Set<String> symbols, String target) {
}
