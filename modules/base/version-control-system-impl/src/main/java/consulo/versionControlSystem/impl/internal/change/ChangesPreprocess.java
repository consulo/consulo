/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change;

import consulo.versionControlSystem.FilePath;
import consulo.logging.Logger;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Irina.Chernushina
 * @since 2012-03-28
 */
public class ChangesPreprocess {
  private static final Logger LOG = Logger.getInstance(ChangesPreprocess.class);

  public static List<Change> preprocessChangesRemoveDeletedForDuplicateMoved(List<Change> list) {
    List<Change> result = new ArrayList<Change>();
    Map<FilePath, Change> map = new HashMap<FilePath, Change>();
    for (Change change : list) {
      if (change.getBeforeRevision() == null) {
        result.add(change);
      } else {
        FilePath beforePath = ChangesUtil.getBeforePath(change);
        Change existing = map.get(beforePath);
        if (existing == null) {
          map.put(beforePath, change);
          continue;
        }
        if (change.getAfterRevision() == null && existing.getAfterRevision() == null) continue;
        if (change.getAfterRevision() != null && existing.getAfterRevision() != null) {
          LOG.error("Incorrect changes list: " + list);
        }
        if (existing.getAfterRevision() != null && change.getAfterRevision() == null) {
          continue; // skip delete change
        }
        if (change.getAfterRevision() != null && existing.getAfterRevision() == null) {
          map.put(beforePath, change);  // skip delete change
        }
      }
    }
    result.addAll(map.values());
    return result;
  }
}
