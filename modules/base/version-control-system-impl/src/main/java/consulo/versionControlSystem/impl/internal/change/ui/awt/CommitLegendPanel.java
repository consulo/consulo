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

package consulo.versionControlSystem.impl.internal.change.ui.awt;

import consulo.virtualFileSystem.status.FileStatus;
import consulo.versionControlSystem.VcsBundle;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;

import javax.swing.*;

public class CommitLegendPanel {

  @Nonnull
  private final SimpleColoredComponent myRootPanel;
  @Nonnull
  private final InfoCalculator myInfoCalculator;

  public CommitLegendPanel(@Nonnull InfoCalculator infoCalculator) {
    myInfoCalculator = infoCalculator;
    myRootPanel = new SimpleColoredComponent();
  }

  @Nonnull
  public JComponent getComponent() {
    return myRootPanel;
  }

  public void update() {
    myRootPanel.clear();
    appendText(myInfoCalculator.getNew(), myInfoCalculator.getIncludedNew(), FileStatus.ADDED, VcsBundle.message("commit.legend.new"));
    appendText(myInfoCalculator.getModified(), myInfoCalculator.getIncludedModified(), FileStatus.MODIFIED, VcsBundle.message("commit.legend.modified"));
    appendText(myInfoCalculator.getDeleted(), myInfoCalculator.getIncludedDeleted(), FileStatus.DELETED, VcsBundle.message("commit.legend.deleted"));
    appendText(myInfoCalculator.getUnversioned(), myInfoCalculator.getIncludedUnversioned(), FileStatus.UNKNOWN,
               VcsBundle.message("commit.legend.unversioned"));
  }

  protected void appendText(int total, int included, @Nonnull FileStatus fileStatus, @Nonnull String labelName) {
    if (total > 0) {
      if (!isPanelEmpty()) {
        appendSpace();
      }
      String pattern = total == included ? "%s %d" : "%s %d of %d";
      String text = String.format(pattern, labelName, included, total);
      myRootPanel.append(text, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, TargetAWT.to(fileStatus.getColor())));
    }
  }

  private boolean isPanelEmpty() {
    return !myRootPanel.iterator().hasNext();
  }

  protected final void appendSpace() {
    myRootPanel.append("   ");
  }

  public interface InfoCalculator {
    int getNew();
    int getModified();
    int getDeleted();
    int getUnversioned();
    int getIncludedNew();
    int getIncludedModified();
    int getIncludedDeleted();
    int getIncludedUnversioned();
  }
}
