/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.execution.test;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.tree.NodeDescriptor;

import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * @author anna
 * @since 2011-10-19
 */
public class TrackRunningTestUtil {
  private TrackRunningTestUtil() {
  }

  public static void installStopListeners(final JTree tree, Disposable parentDisposable, final Consumer<AbstractTestProxy> setSelection) {
    final ClickListener userSelectionListener = new ClickListener() {
      @Override
      public boolean onClick(MouseEvent e, int clickCount) {
        setSelection.accept(setUserSelection(tree.getPathForLocation(e.getX(), e.getY())));
        return true;
      }
    };
    userSelectionListener.installOn(tree);
    final KeyAdapter keyAdapter = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_RIGHT) {
          setSelection.accept(setUserSelection(tree.getSelectionPath()));
        }
      }
    };
    tree.addKeyListener(keyAdapter);
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        userSelectionListener.uninstall(tree);
        tree.removeKeyListener(keyAdapter);
      }
    });
  }

  @Nullable
  private static AbstractTestProxy setUserSelection(TreePath treePath) {
    if (treePath != null) {
      Object component = treePath.getLastPathComponent();
      if (component instanceof DefaultMutableTreeNode) {
        Object userObject = ((DefaultMutableTreeNode)component).getUserObject();
        if (userObject instanceof NodeDescriptor) {
          return (AbstractTestProxy)((NodeDescriptor)userObject).getElement();
        }
      }
    }
    return null;
  }
}
