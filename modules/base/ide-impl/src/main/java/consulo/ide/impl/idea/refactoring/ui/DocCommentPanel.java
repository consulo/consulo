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

/*
 * Created by IntelliJ IDEA.
 * User: dsl
 * Date: 17.06.2002
 * Time: 20:38:33
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package consulo.ide.impl.idea.refactoring.ui;

import consulo.ide.impl.idea.refactoring.util.DocCommentPolicy;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class DocCommentPanel extends JPanel {
  private JRadioButton myRbJavaDocAsIs = null;
  private JRadioButton myRbJavaDocMove = null;
  private JRadioButton myRbJavaDocCopy = null;
  private final TitledBorder myBorder;

  public DocCommentPanel(String title) {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    myBorder = IdeBorderFactory.createTitledBorder(
      title,
      true,
      new Insets(
        IdeBorderFactory.TITLED_BORDER_TOP_INSET,
        UIUtil.DEFAULT_HGAP,
        IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET,
        IdeBorderFactory.TITLED_BORDER_RIGHT_INSET
      )
    );
    this.setBorder(myBorder);

    myRbJavaDocAsIs = new JRadioButton();
    myRbJavaDocAsIs.setText(RefactoringLocalize.javadocAsIs().get());
    add(myRbJavaDocAsIs);
    myRbJavaDocAsIs.setFocusable(false);

    myRbJavaDocCopy = new JRadioButton();
    myRbJavaDocCopy.setText(RefactoringLocalize.javadocCopy().get());
    myRbJavaDocCopy.setFocusable(false);
    add(myRbJavaDocCopy);

    myRbJavaDocMove = new JRadioButton();
    myRbJavaDocMove.setText(RefactoringLocalize.javadocMove().get());
    myRbJavaDocMove.setFocusable(false);
    add(myRbJavaDocMove);

    ButtonGroup bg = new ButtonGroup();
    bg.add(myRbJavaDocAsIs);
    bg.add(myRbJavaDocCopy);
    bg.add(myRbJavaDocMove);
    bg.setSelected(myRbJavaDocMove.getModel(), true);
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension preferredSize = super.getPreferredSize();
    final Dimension borderSize = myBorder.getMinimumSize(this);
    return new Dimension(
      Math.max(preferredSize.width, borderSize.width + 10),
      Math.max(preferredSize.height, borderSize.height)
    );
  }

  public void setPolicy(final int javaDocPolicy) {
    if (javaDocPolicy == DocCommentPolicy.COPY) {
      myRbJavaDocCopy.setSelected(true);
    }
    else if (javaDocPolicy == DocCommentPolicy.MOVE) {
      myRbJavaDocMove.setSelected(true);
    }
    else {
      myRbJavaDocAsIs.setSelected(true);
    }
  }

  public int getPolicy() {
    if (myRbJavaDocCopy != null && myRbJavaDocCopy.isSelected()) {
      return DocCommentPolicy.COPY;
    }
    if (myRbJavaDocMove != null && myRbJavaDocMove.isSelected()) {
      return DocCommentPolicy.MOVE;
    }

    return DocCommentPolicy.ASIS;
  }
}
