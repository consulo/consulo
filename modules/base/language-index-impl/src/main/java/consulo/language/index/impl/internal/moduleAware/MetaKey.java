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
package consulo.language.index.impl.internal.moduleAware;

/**
 * Composite key for {@link ModuleAwareIndexMetaStorage}: identifies one entry as
 * "the stored {@link OptionsMeta} for file {@code fileId} under index {@code indexUniqueId}".
 *
 * <p>{@code indexUniqueId} comes from {@link consulo.index.io.ID#getUniqueId()}.</p>
 */
public record MetaKey(int indexUniqueId, int fileId) {
}
