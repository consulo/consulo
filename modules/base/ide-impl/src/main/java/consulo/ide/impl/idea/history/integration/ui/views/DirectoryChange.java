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

package consulo.ide.impl.idea.history.integration.ui.views;

import consulo.ide.impl.idea.history.core.revisions.Difference;
import consulo.ide.impl.idea.history.integration.ui.models.DirectoryChangeModel;
import consulo.ide.impl.idea.openapi.vcs.changes.Change;

public class DirectoryChange extends Change {
  private final DirectoryChangeModel myModel;

  public DirectoryChange(DirectoryChangeModel m) {
    super(m.getContentRevision(0), m.getContentRevision(1));
    myModel = m;
  }

  public DirectoryChangeModel getModel() {
    return myModel;
  }

  public boolean canShowFileDifference() {
    return myModel.canShowFileDifference();
  }

  public Difference getDifference() {
    return myModel.getDifference();
  }
}
