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
package consulo.content.platform;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PlatformTargetManager {
    /**
     * The local machine first, then every contributed target.
     */
    List<PlatformTarget> getTargets();

    PlatformTarget getLocalTarget();

    /**
     * The target with this id, the local one for a null id, null when no target has it any more.
     */
    @Nullable PlatformTarget findTarget(@Nullable String id);

    /**
     * The reachable target whose file system the path belongs to - how a path chosen on some machine names it.
     */
    @Nullable PlatformTarget findTargetOf(Path path);
}
