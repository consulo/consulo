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
package consulo.component.store.internal;

/**
 * The storage cannot be reached now - it is on a machine which is not connected. Its state is unknown, not empty: a
 * component which could not read it keeps its default state, and is not saved until its state is read.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
public class StateStorageUnavailableException extends RuntimeException {
    public StateStorageUnavailableException(String message) {
        super(message);
    }
}
