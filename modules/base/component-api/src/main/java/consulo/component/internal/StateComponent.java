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
package consulo.component.internal;

import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.PersistentStateComponentAsync;

/**
 * Common supertype of the synchronous and asynchronous state component contracts, so the store can hold
 * both kinds in one field. Not part of the public API - implement {@link PersistentStateComponent} or
 * {@link PersistentStateComponentAsync} instead.
 *
 * @author VISTALL
 * @since 2026-07-29
 */
public sealed interface StateComponent permits PersistentStateComponent, PersistentStateComponentAsync {
}
