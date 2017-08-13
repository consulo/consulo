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
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.maddyhome.idea.copyright.CopyrightManager;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.pattern.EntityUtil;
import com.maddyhome.idea.copyright.pattern.VelocityHelper;
import consulo.annotations.RequiredDispatchThread;
import consulo.copyright.PredefinedCopyrightTextEP;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CopyrightConfigurable extends NamedConfigurable<CopyrightProfile> implements Configurable.NoScroll {
  public static class PreviewDialog extends DialogWrapper {
    @NotNull
    private final String myText;
    private final Dimension mySize;

    private Editor myEditor;

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
      return new Action[]{getOKAction()};
    }

    @Override
    protected void dispose() {
      super.dispose();
      if (myEditor != null) {
        EditorFactory.getInstance().releaseEditor(myEditor);
      }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      Document document = EditorFactory.getInstance().createDocument(myText);
      myEditor = EditorFactory.getInstance().createViewer(document);
      setupEditor(myEditor);

      JComponent component = myEditor.getComponent();
      component.setPreferredSize(mySize);
      return component;
    }
  }

  private final CopyrightProfile myCopyrightProfile;
  private final Project myProject;
  private JPanel myWholePanel;
  private boolean myModified;
  private String myDisplayName;
  private JTextField myKeywordField;
  private JTextField myAllowReplaceTextField;

  private Editor myCopyrightEditor;
  private Document myCopyrightDocument;

  public CopyrightConfigurable(final Project project, CopyrightProfile copyrightProfile, Runnable updater) {
    super(true, updater);
    myProject = project;
    myCopyrightProfile = copyrightProfile;
    myDisplayName = myCopyrightProfile.getName();

    myWholePanel = new JPanel(new VerticalFlowLayout());

    EditorFactory editorFactory = EditorFactory.getInstance();
    myCopyrightDocument = editorFactory.createDocument("");
    myCopyrightEditor = editorFactory.createEditor(myCopyrightDocument);
    setupEditor(myCopyrightEditor);

    DefaultActionGroup group = new DefaultActionGroup();

    JComponent editorComponent = myCopyrightEditor.getComponent();

    group.add(new AnAction("Preview", null, AllIcons.Actions.Preview) {
      @RequiredDispatchThread
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        try {
          String evaluate = VelocityHelper.evaluate(null, project, null, myCopyrightEditor.getDocument().getText());

          new PreviewDialog(project, evaluate, editorComponent.getSize()).show();
        }
        catch (Exception e1) {
          Messages.showErrorDialog(myProject, "Template contains error:\n" + e1.getMessage(), "Preview");
        }
      }
    });
    PredefinedCopyrightTextEP[] extensions = PredefinedCopyrightTextEP.EP_NAME.getExtensions();
    if (extensions.length > 0) {
      group.add(new AnAction("Reset To", null, AllIcons.Actions.Reset) {
        @RequiredDispatchThread
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          DefaultActionGroup actionGroup = new DefaultActionGroup();
          for (PredefinedCopyrightTextEP extension : extensions) {
            actionGroup.add(new AnAction(extension.name) {
              @RequiredDispatchThread
              @Override
              public void actionPerformed(@NotNull AnActionEvent e) {
                String text = extension.getText();
                WriteAction.run(() -> myCopyrightDocument.setText(text));
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
    result.add(editorComponent, BorderLayout.CENTER);
    result.setPreferredSize(JBUI.size(-1, 400));

    myWholePanel.add(result);

    myWholePanel.add(LabeledComponent.left(myKeywordField = new JBTextField(), "Keyword to detect copyright in comments"));
    myWholePanel.add(LabeledComponent.left(myAllowReplaceTextField = new JBTextField(), "Allow replacing copyright if old copyright contains"));
  }

  static void setupEditor(Editor editor) {
    EditorSettings settings = editor.getSettings();
    settings.setLineNumbersShown(false);
    settings.setFoldingOutlineShown(false);
    settings.setIndentGuidesShown(false);
    settings.setLineMarkerAreaShown(false);
  }

  @Override
  public CopyrightProfile getEditableObject() {
    return myCopyrightProfile;
  }

  @Override
  public String getBannerSlogan() {
    return myCopyrightProfile.getName();
  }

  @Override
  public JComponent createOptionsPanel() {
    return myWholePanel;
  }

  @Override
  @Nls
  public String getDisplayName() {
    return myCopyrightProfile.getName();
  }

  @Override
  public void setDisplayName(String s) {
    myCopyrightProfile.setName(s);
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    return null;
  }

  @Override
  @RequiredDispatchThread
  public boolean isModified() {
    return myModified ||
           !Comparing.strEqual(EntityUtil.encode(myCopyrightDocument.getText().trim()), myCopyrightProfile.getNotice()) ||
           !Comparing.strEqual(myKeywordField.getText().trim(), myCopyrightProfile.getKeyword()) ||
           !Comparing.strEqual(myAllowReplaceTextField.getText().trim(), myCopyrightProfile.getAllowReplaceKeyword()) ||
           !Comparing.strEqual(myDisplayName, myCopyrightProfile.getName());
  }

  public void setModified(boolean modified) {
    myModified = modified;
  }

  @Override
  @RequiredDispatchThread
  public void apply() throws ConfigurationException {
    myCopyrightProfile.setNotice(EntityUtil.encode(myCopyrightDocument.getText().trim()));
    myCopyrightProfile.setKeyword(myKeywordField.getText());
    myCopyrightProfile.setAllowReplaceKeyword(myAllowReplaceTextField.getText());
    CopyrightManager.getInstance(myProject).replaceCopyright(myDisplayName, myCopyrightProfile);
    myDisplayName = myCopyrightProfile.getName();
    myModified = false;
  }

  @Override
  @RequiredDispatchThread
  public void reset() {
    myDisplayName = myCopyrightProfile.getName();
    WriteAction.run(() -> myCopyrightDocument.setText(EntityUtil.decode(myCopyrightProfile.getNotice())));
    myKeywordField.setText(myCopyrightProfile.getKeyword());
    myAllowReplaceTextField.setText(myCopyrightProfile.getAllowReplaceKeyword());
  }

  @RequiredDispatchThread
  @Override
  public void disposeUIResources() {
    EditorFactory.getInstance().releaseEditor(myCopyrightEditor);
  }
}
