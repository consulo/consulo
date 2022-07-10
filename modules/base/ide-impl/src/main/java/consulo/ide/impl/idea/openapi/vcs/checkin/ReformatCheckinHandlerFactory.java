/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import consulo.vcs.checkin.CheckinHandlerFactory;
import consulo.vcs.checkin.CheckinProjectPanel;
import consulo.vcs.change.CommitContext;
import consulo.vcs.checkin.CheckinHandler;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
@ExtensionImpl(id = "reformat", order = "first")
public class ReformatCheckinHandlerFactory extends CheckinHandlerFactory {
  @Override
  @Nonnull
  public CheckinHandler createHandler(final CheckinProjectPanel panel, CommitContext commitContext) {
    return new ReformatBeforeCheckinHandler(panel.getProject(), panel);
  }
}