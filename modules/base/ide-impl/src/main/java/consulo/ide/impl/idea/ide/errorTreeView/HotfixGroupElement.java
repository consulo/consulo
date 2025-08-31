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
package consulo.ide.impl.idea.ide.errorTreeView;

import consulo.ide.impl.idea.ui.CustomizeColoredTreeCellRenderer;
import consulo.application.AllIcons;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.errorTreeView.HotfixGate;
import consulo.ui.ex.errorTreeView.MutableErrorTreeView;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;
import java.util.function.Consumer;

public class HotfixGroupElement extends GroupingElement {

  private final Consumer<HotfixGate> myHotfix;
  private final String myFixDescription;
  private final MutableErrorTreeView myView;
  private boolean myInProgress;
  private final CustomizeColoredTreeCellRenderer myLeftTreeCellRenderer;
  private final CustomizeColoredTreeCellRenderer myRightTreeCellRenderer;

  public HotfixGroupElement(String name, Object data, VirtualFile file, Consumer<HotfixGate> hotfix, String fixDescription, MutableErrorTreeView view) {
    super(name, data, file);
    myHotfix = hotfix;
    myFixDescription = fixDescription;
    myView = view;
    myLeftTreeCellRenderer = new CustomizeColoredTreeCellRenderer() {
      @Override
      public void customizeCellRenderer(SimpleColoredComponent renderer, JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        renderer.setIcon(AllIcons.General.Error);

        String[] text = getText();
        String errorText = ((text != null) && (text.length > 0)) ? text[0] : "";
        renderer.append("Error: " + errorText, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
    };
    myRightTreeCellRenderer = new MyRightRenderer();
  }

  private class MyRightRenderer extends CustomizeColoredTreeCellRenderer {
    private final HotfixGroupElement.MyRunner myRunner;

    public MyRightRenderer() {
      myRunner = new MyRunner();
    }

    @Override
    public void customizeCellRenderer(SimpleColoredComponent renderer, JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      renderer.append(" ");
      if (myInProgress) {
        renderer.append("fixing...", SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES);
      }
      else {
        renderer.append("Fix: " + myFixDescription, SimpleTextAttributes.LINK_BOLD_ATTRIBUTES, myRunner);
      }
    }

    @Override
    public Object getTag() {
      return myRunner;
    }
  }

  // we can inherit from HaveTooltip here
  @Override
  public CustomizeColoredTreeCellRenderer getLeftSelfRenderer() {
    return myLeftTreeCellRenderer;
  }

  @Override
  public CustomizeColoredTreeCellRenderer getRightSelfRenderer() {
    return myRightTreeCellRenderer;
  }

  private class MyRunner implements Runnable {
    private MyRunner() {
    }

    // todo name can be an ID
    @Override
    public void run() {
      myInProgress = true;
      myView.reload();
      String name = getName();
      myHotfix.accept(new HotfixGate(name, myView));
    }
  }
}
