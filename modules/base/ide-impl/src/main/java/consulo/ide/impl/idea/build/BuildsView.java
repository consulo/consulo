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
package consulo.ide.impl.idea.build;

import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.event.BuildEvent;
import consulo.disposer.Disposable;
import consulo.ui.ex.content.Content;

import java.util.Set;

/**
 * What the manager of builds needs from whatever shows them, with nothing of the toolkit drawing it.
 *
 * @author VISTALL
 * @since 2026-09-23
 */
public interface BuildsView extends Disposable {
    Content getContent();

    Set<BuildDescriptor> getBuildDescriptors();

    boolean shouldConsume(Object buildId);

    void onEvent(Object buildId, BuildEvent event);
}
