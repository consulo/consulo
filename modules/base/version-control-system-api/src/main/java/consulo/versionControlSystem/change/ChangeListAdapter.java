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

package consulo.versionControlSystem.change;

import consulo.annotation.DeprecationInfo;

import java.util.Collection;

/**
 * @author yole
 */
@Deprecated
@DeprecationInfo("Use ChangeListListener")
public class ChangeListAdapter implements ChangeListListener {
  @Override
  public void changeListAdded(ChangeList list) {
  }

  @Override
  public void changeListRemoved(ChangeList list) {
  }

  @Override
  public void changeListChanged(ChangeList list) {
  }

  @Override
  public void changeListRenamed(ChangeList list, String oldName) {
  }

  @Override
  public void changeListCommentChanged(final ChangeList list, final String oldComment) {
  }

  @Override
  public void changesMoved(Collection<Change> changes, ChangeList fromList, ChangeList toList) {
  }

  @Override
  public void defaultListChanged(final ChangeList oldDefaultList, ChangeList newDefaultList) {
  }

  @Override
  public void unchangedFileStatusChanged() {
  }

  @Override
  public void changeListUpdateDone() {
  }

  @Override
  public void changesRemoved(final Collection<Change> changes, final ChangeList fromList) {
  }

  @Override
  public void changesAdded(Collection<Change> changes, ChangeList toList) {
  }
}