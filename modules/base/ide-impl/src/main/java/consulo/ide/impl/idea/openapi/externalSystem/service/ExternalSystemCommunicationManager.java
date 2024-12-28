/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.externalSystem.service;

import consulo.externalSystem.model.ProjectSystemId;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * External system integration consists of common 'external system' functionality and external system-specific code. There are at
 * least two approaches how to work with that external system-specific code:
 * <pre>
 * <ul>
 *   <li>use it from the ide process;</li>
 *   <li>create a slave process and perform external system-specific actions there;</li>
 * </ul>
 * </pre>
 * <p/>
 * E.g. when we work with particular external system api it might worth to do that at a separate process in order to avoid bad stuff
 * like memory leaks to happen at the ide process. However, when external system-specific communication just starts new external
 * system-process, there is no point in creating intermediate mediator process just to launch new process.
 * <p/>
 * That's why that stuff is covered by the current interface, i.e. different implementations are supposed to provide
 * different 'in process' modes.
 *
 * @author Denis Zhdanov
 * @since 8/9/13 3:21 PM
 */
public interface ExternalSystemCommunicationManager {
    /**
     * Creates new external system facade for the given arguments.
     *
     * @param id               if for which new facade is to be created
     * @param externalSystemId target external system id
     * @return newly created facade for the given arguments (if it was possible to create one)
     * @throws Exception in case something goes wrong
     */
    @Nullable
    RemoteExternalSystemFacade acquire(@Nonnull String id, @Nonnull ProjectSystemId externalSystemId) throws Exception;

    /**
     * Release resource acquired by the current manager
     *
     * @param id               resource id
     * @param externalSystemId target external system id
     * @throws Exception in case something goes wrong
     */
    void release(@Nonnull String id, @Nonnull ProjectSystemId externalSystemId) throws Exception;

    boolean isAlive(@Nonnull RemoteExternalSystemFacade facade);

    /**
     * Disposes all resources acquired by the current manager.
     */
    void clear();
}
