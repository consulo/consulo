/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.checkin;

import consulo.annotation.component.ExtensionImpl;
import consulo.versionControlSystem.checkin.CheckinHandlerFactory;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.checkin.CheckinHandler;

import jakarta.annotation.Nonnull;

/**
 * @author irengrig
 * @since 2011-02-17
 */
@ExtensionImpl(id = "todo", order = "after code-analysis")
public class TodoCheckinHandlerFactory extends CheckinHandlerFactory {
  @Nonnull
  @Override
  public CheckinHandler createHandler(CheckinProjectPanel panel, CommitContext commitContext) {
    return new TodoCheckinHandler(panel);
  }
}
