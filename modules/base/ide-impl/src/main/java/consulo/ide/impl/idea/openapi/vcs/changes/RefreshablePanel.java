package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.disposer.Disposable;

import javax.swing.*;

/**
 * @author irengrig
 *         Date: 8/12/11
 *         Time: 6:47 PM
 */
public interface RefreshablePanel<Data> extends Disposable {
  boolean refreshDataSynch();
  void dataChanged();
  void refresh();
  JPanel getPanel();
  void away();
  boolean isStillValid(Data data);
}
