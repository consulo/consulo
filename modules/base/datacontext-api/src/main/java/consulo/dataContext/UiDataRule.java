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
package consulo.dataContext;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import jakarta.annotation.Nonnull;

/**
 * Extension point for computing derived data from an already-collected snapshot.
 * <p>
 * Unlike {@link GetDataRule} which works with {@link DataProvider}, this rule
 * works with the {@link UiDataProvider} / {@link DataSink} system. Rules can
 * read already-collected immediate data via {@link DataSnapshot} and contribute
 * additional data (including lazy/deferred values) via {@link DataSink}.
 * <p>
 * Example: computing {@code PSI_FILE} from {@code VIRTUAL_FILE} + {@code PROJECT}.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface UiDataRule {
    void uiDataSnapshot(@Nonnull DataSink sink, @Nonnull DataSnapshot snapshot);
}
