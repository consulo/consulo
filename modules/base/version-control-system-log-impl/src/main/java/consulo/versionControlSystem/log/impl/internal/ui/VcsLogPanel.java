/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.dataContext.DataProvider;
import consulo.ui.ex.awt.JBPanel;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.log.impl.internal.VcsLogManager;
import org.jspecify.annotations.Nullable;

import java.awt.*;

public class VcsLogPanel extends JBPanel implements DataProvider {
  
  private final VcsLogManager myManager;

  public VcsLogPanel(VcsLogManager manager, VcsLogUiImpl logUi) {
    super(new BorderLayout());
    myManager = manager;
    add(logUi.getMainFrame().getMainComponent(), BorderLayout.CENTER);
  }

  @Nullable
  @Override
  public Object getData(Key dataId) {
    if (VcsLogInternalDataKeys.LOG_MANAGER == dataId) {
      return myManager;
    }
    return null;
  }
}
