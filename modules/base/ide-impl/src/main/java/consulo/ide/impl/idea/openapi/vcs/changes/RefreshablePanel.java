package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.disposer.Disposable;

import javax.swing.*;

/**
 * @author irengrig
 * @since 2011-08-12
 */
public interface RefreshablePanel<Data> extends Disposable {
  boolean refreshDataSynch();
  void dataChanged();
  void refresh();
  JPanel getPanel();
  void away();
  boolean isStillValid(Data data);
}
