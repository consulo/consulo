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
package consulo.externalSystem.impl.internal.service;

/**
 * Replaces {@link java.rmi.RemoteException} for external system communication failures.
 *
 * @author VISTALL
 * @since 2026-03-20
 */
public class ExternalSystemCommunicationException extends RuntimeException {
    public ExternalSystemCommunicationException(String message) {
        super(message);
    }

    public ExternalSystemCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalSystemCommunicationException(Throwable cause) {
        super(cause);
    }
}
