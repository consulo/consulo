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
package consulo.remoteServer.agent.impl.internal;

import io.grpc.Status;
import org.jspecify.annotations.Nullable;

public class RemotePermissionException extends RemoteAgentException {
    private final String myPermission;

    public RemotePermissionException(String message, String permission, @Nullable Throwable cause) {
        super(message, Status.Code.PERMISSION_DENIED, cause);
        myPermission = permission;
    }

    public String getPermission() {
        return myPermission;
    }
}
