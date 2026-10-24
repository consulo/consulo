// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.externalProcessAuthHelper;

/**
 * Flag to get user credentials in different interaction modes:
 * NONE: no authentication will be performed if password is requested
 * (Also, native credential helper should be disabled manually as a command parameter);
 * SILENT: the IDE will look for passwords in the common password storages. if no password is found, no authentication will be performed;
 * FULL: the IDE will look for passwords in the common password storages. If no password is found, an authentication dialog will be displayed.
 * <p>
 * Note that even if the authentication mode is set to {@link AuthenticationMode#SILENT},
 * the IDE can still trigger the OS prompt for authentication (e.g., macOS Keychain).
 */
public enum AuthenticationMode {
    NONE,
    SILENT,
    FULL
}
