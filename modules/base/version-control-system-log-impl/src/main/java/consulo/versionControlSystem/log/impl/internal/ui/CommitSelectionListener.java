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

import com.google.common.primitives.Ints;
import consulo.application.ApplicationManager;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBLoadingPanel;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import org.jspecify.annotations.Nullable;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.List;

public abstract class CommitSelectionListener implements ListSelectionListener {
  private final static Logger LOG = Logger.getInstance(CommitSelectionListener.class);
  
  private final VcsLogDataImpl myLogData;
  
  protected final VcsLogGraphTable myGraphTable;
  
  private final JBLoadingPanel myLoadingPanel;

  @Nullable private ListSelectionEvent myLastEvent;
  @Nullable
  private ProgressIndicator myLastRequest;

  protected CommitSelectionListener(VcsLogDataImpl data, VcsLogGraphTable table, JBLoadingPanel panel) {
    myLogData = data;
    myGraphTable = table;
    myLoadingPanel = panel;
  }

  @Override
  public void valueChanged(@Nullable ListSelectionEvent event) {
    if (event != null && event.getValueIsAdjusting()) return;

    myLastEvent = event;
    if (myLastRequest != null) myLastRequest.cancel();
    myLastRequest = null;

    ApplicationManager.getApplication().invokeLater(this::processEvent, () -> myLastEvent != event);
  }

  public void processEvent() {
    int rows = myGraphTable.getSelectedRowCount();
    if (rows < 1) {
      myLoadingPanel.stopLoading();
      onEmptySelection();
    }
    else {
      onSelection(myGraphTable.getSelectedRows());
      myLoadingPanel.startLoading();

      EmptyProgressIndicator indicator = new EmptyProgressIndicator();
      myLastRequest = indicator;

      List<Integer> selectionToLoad = getSelectionToLoad();
      myLogData.getCommitDetailsGetter()
        .loadCommitsData(myGraphTable.getModel().convertToCommitIds(selectionToLoad), detailsList -> {
          if (myLastRequest == indicator && !(indicator.isCanceled())) {
            LOG.assertTrue(selectionToLoad.size() == detailsList.size(),
                           "Loaded incorrect number of details " + detailsList + " for selection " + selectionToLoad);
            myLastRequest = null;
            onDetailsLoaded(detailsList);
            myLoadingPanel.stopLoading();
          }
        }, indicator);
    }
  }

  
  protected List<Integer> getSelectionToLoad() {
    return Ints.asList(myGraphTable.getSelectedRows());
  }

  @RequiredUIAccess
  protected abstract void onDetailsLoaded(List<VcsFullCommitDetails> detailsList);

  @RequiredUIAccess
  protected abstract void onSelection(int[] selection);

  @RequiredUIAccess
  protected abstract void onEmptySelection();
}