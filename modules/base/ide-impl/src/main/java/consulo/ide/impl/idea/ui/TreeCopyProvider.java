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

package consulo.ide.impl.idea.ui;

import consulo.ui.ex.CopyProvider;
import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.datatransfer.Clipboard;

/**
 * @author yole
 */
public class TreeCopyProvider implements CopyProvider {
  private static final Logger LOG = Logger.getInstance(TreeCopyProvider.class);
  private final JTree myTree;

  public TreeCopyProvider(JTree tree) {
    myTree = tree;
  }

  public void performCopy(@Nonnull DataContext dataContext) {
    try {
      Clipboard clipboard = myTree.getToolkit().getSystemClipboard();
      myTree.getTransferHandler().exportToClipboard(myTree, clipboard, TransferHandler.COPY);
    }
    catch(Exception ex) {
      // probably don't have clipboard access or something
      LOG.info(ex);
    }
  }

  public boolean isCopyEnabled(@Nonnull DataContext dataContext) {
    return myTree.getSelectionPath() != null;
  }

  public boolean isCopyVisible(@Nonnull DataContext dataContext) {
    return true;
  }
}
