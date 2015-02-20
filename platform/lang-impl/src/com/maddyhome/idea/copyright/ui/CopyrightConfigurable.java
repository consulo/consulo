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

package com.maddyhome.idea.copyright.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.maddyhome.idea.copyright.CopyrightManager;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.PredefinedCopyrightTextEP;
import com.maddyhome.idea.copyright.pattern.EntityUtil;
import com.maddyhome.idea.copyright.pattern.VelocityHelper;
import lombok.val;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CopyrightConfigurable extends NamedConfigurable<CopyrightProfile> {
  public static class PreviewDialog extends DialogWrapper {
    @NotNull
    private final String myText;
    private final Dimension mySize;

    public PreviewDialog(@Nullable Project project, @NotNull String text, @NotNull Dimension size) {
      super(project);
      myText = text;
      mySize = size;
      setTitle("Preview");
      init();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
      return new Action[] {getOKAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      JEditorPane editorPane = new JEditorPane();
      editorPane.setFont(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));
      editorPane.setText(myText);
      editorPane.setPreferredSize(mySize);
      editorPane.setEditable(false);

      return new JBScrollPane(editorPane);
    }
  }

  private final CopyrightProfile myCopyrightProfile;
  private final Project myProject;
  private JPanel myWholePanel;
  private boolean myModified;
  private String myDisplayName;
  private JEditorPane myCopyrightEditorPanel;
  private JTextField myKeywordField;
  private JTextField myAllowReplaceTextField;

  public CopyrightConfigurable(final Project project, CopyrightProfile copyrightProfile, Runnable updater) {
    super(true, updater);
    myProject = project;
    myCopyrightProfile = copyrightProfile;
    myDisplayName = myCopyrightProfile.getName();

    myWholePanel = new JPanel(new VerticalFlowLayout());

    myCopyrightEditorPanel = new JEditorPane();
    myCopyrightEditorPanel.setFont(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));

    DefaultActionGroup group = new DefaultActionGroup();

    group.add(new AnAction("Preview", null, AllIcons.Actions.Preview) {
      @Override
      public void actionPerformed(AnActionEvent e) {
        try {
          String evaluate = VelocityHelper.evaluate(null, project, null, myCopyrightEditorPanel.getText());

          new PreviewDialog(project, evaluate, myCopyrightEditorPanel.getSize()).show();
        }
        catch (Exception e1) {
          Messages.showErrorDialog(myProject, "Template contains error:\n" + e1.getMessage(), "Preview");
        }
      }
    });
    val extensions = PredefinedCopyrightTextEP.EP_NAME.getExtensions();
    if (extensions.length > 0) {
      group.add(new AnAction("Reset To", null, AllIcons.Actions.Reset) {
        @Override
        public void actionPerformed(AnActionEvent e) {
          val actionGroup = new DefaultActionGroup();
          for (val extension : extensions) {
            actionGroup.add(new AnAction(extension.name) {
              @Override
              public void actionPerformed(AnActionEvent e) {
                String text = extension.getText();
                myCopyrightEditorPanel.setText(text);
              }
            });
          }
          ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, actionGroup);

          popupMenu.getComponent().show(e.getInputEvent().getComponent(), e.getInputEvent().getComponent().getWidth(), 0);
        }
      });
    }

    ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true);

    JPanel result = new JPanel(new BorderLayout());
    JPanel toolBarPanel = new JPanel(new BorderLayout());
    toolBarPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
    JLabel label = new JBLabel("Velocity Template");
    label.setForeground(JBColor.GRAY);
    toolBarPanel.add(label, BorderLayout.EAST);

    result.add(toolBarPanel, BorderLayout.NORTH);
    result.add(new JBScrollPane(myCopyrightEditorPanel), BorderLayout.CENTER);
    result.setPreferredSize(new Dimension(-1, 400));

    myWholePanel.add(result);

    myWholePanel.add(LabeledComponent.left(myKeywordField = new JBTextField(), "Keyword to detect copyright in comments"));
    myWholePanel.add(LabeledComponent.left(myAllowReplaceTextField = new JBTextField(), "Allow replacing copyright if old copyright contains"));
  }

  public CopyrightProfile getEditableObject() {
    return myCopyrightProfile;
  }

  public String getBannerSlogan() {
    return myCopyrightProfile.getName();
  }

  public JComponent createOptionsPanel() {
    return myWholePanel;
  }

  @Nls
  public String getDisplayName() {
    return myCopyrightProfile.getName();
  }

  public void setDisplayName(String s) {
    myCopyrightProfile.setName(s);
  }

  @Nullable
  @NonNls
  public String getHelpTopic() {
    return null;
  }

  public boolean isModified() {
    return myModified ||
           !Comparing.strEqual(EntityUtil.encode(myCopyrightEditorPanel.getText().trim()), myCopyrightProfile.getNotice()) ||
           !Comparing.strEqual(myKeywordField.getText().trim(), myCopyrightProfile.getKeyword()) ||
           !Comparing.strEqual(myAllowReplaceTextField.getText().trim(), myCopyrightProfile.getAllowReplaceKeyword()) ||
           !Comparing.strEqual(myDisplayName, myCopyrightProfile.getName());
  }

  public void setModified(boolean modified) {
    myModified = modified;
  }

  public void apply() throws ConfigurationException {
    myCopyrightProfile.setNotice(EntityUtil.encode(myCopyrightEditorPanel.getText().trim()));
    myCopyrightProfile.setKeyword(myKeywordField.getText());
    myCopyrightProfile.setAllowReplaceKeyword(myAllowReplaceTextField.getText());
    CopyrightManager.getInstance(myProject).replaceCopyright(myDisplayName, myCopyrightProfile);
    myDisplayName = myCopyrightProfile.getName();
    myModified = false;
  }

  public void reset() {
    myDisplayName = myCopyrightProfile.getName();
    myCopyrightEditorPanel.setText(EntityUtil.decode(myCopyrightProfile.getNotice()));
    myKeywordField.setText(myCopyrightProfile.getKeyword());
    myAllowReplaceTextField.setText(myCopyrightProfile.getAllowReplaceKeyword());
  }

  public void disposeUIResources() {
  }
}
