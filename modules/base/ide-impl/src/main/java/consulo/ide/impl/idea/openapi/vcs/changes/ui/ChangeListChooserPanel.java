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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.codeEditor.EditorEx;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CommittedChangeListRenderer;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.language.editor.ui.awt.StringComboboxEditor;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.impl.idea.util.NullableConsumer;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static consulo.ide.impl.idea.codeInsight.completion.ComboEditorCompletionContributor.CONTINUE_RUN_COMPLETION;

public class ChangeListChooserPanel extends JPanel {

  private final MyEditorComboBox myExistingListsCombo;
  private final NewEditChangelistPanel myListPanel;
  private final NullableConsumer<String> myOkEnabledListener;
  private final Project myProject;
  private String myLastTypedDescription;
  private boolean myNewNameSuggested = false;

  public ChangeListChooserPanel(final Project project, @Nonnull final NullableConsumer<String> okEnabledListener) {
    super(new BorderLayout());
    myProject = project;
    myExistingListsCombo = new MyEditorComboBox(project);
    myExistingListsCombo.setEditable(true);
    myExistingListsCombo.setRenderer(new ColoredListCellRenderer<String>() {
      @Override
      protected void customizeCellRenderer(@Nonnull JList<? extends String> list,
                                           String value,
                                           int index,
                                           boolean selected,
                                           boolean hasFocus) {
        if (value != null) {
          String name = value;
          LocalChangeList changeList = ChangeListManager.getInstance(myProject).findChangeList(name);
          int visibleWidth = getSize().width;
          if (visibleWidth == 0) {
            visibleWidth = MyEditorComboBox.PREF_WIDTH;
          }
          final FontMetrics fm = list.getFontMetrics(list.getFont());
          final int width = fm.stringWidth(name);
          if (width > visibleWidth) {
            final String truncated = CommittedChangeListRenderer
                    .truncateDescription(name, fm, visibleWidth - fm.stringWidth(" ..") - 7);
            if (truncated.length() > 5) {
              name = truncated + " ..";
            }
          }
          append(name, changeList != null && changeList.isDefault()
                       ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                       : SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
      }
    });
    myListPanel = new NewEditChangelistPanel(myProject) {

      @Override
      protected NewEditChangelistPanel.ComponentWithTextFieldWrapper createComponentWithTextField(Project project) {
        return new ComponentWithTextFieldWrapper(myExistingListsCombo) {
          @Nonnull
          @Override
          public EditorTextField getEditorTextField() {
            return myExistingListsCombo.getEditorTextField();
          }
        };
      }

      @Override
      @RequiredUIAccess
      protected void nameChanged(String errorMessage) {
        //invoke later because of undo manager problem: when you try to undo changelist after description was already changed manually
        ApplicationManager.getApplication().invokeLater(() -> updateDescription(), IdeaModalityState.current());
        myOkEnabledListener.consume(errorMessage);
      }

      @Override
      public void init(LocalChangeList initial) {
        super.init(initial);
        myDescriptionTextArea.addFocusListener(new FocusAdapter() {
          @Override
          public void focusLost(FocusEvent e) {
            super.focusLost(e);
            if (getExistingChangelistByName(myListPanel.getChangeListName()) == null) {
              myLastTypedDescription = myListPanel.getDescription();
            }
          }
        });
      }

      @Override
      protected void nameChangedImpl(Project project, LocalChangeList initial) {
        nameChanged(StringUtil.isEmptyOrSpaces(getChangeListName()) ? "Cannot create new changelist with empty name." : null);
      }
    };
    myOkEnabledListener = okEnabledListener;
    add(myListPanel, BorderLayout.CENTER);
  }

  public void init() {
    myListPanel.init(null);
  }

  public void setChangeLists(Collection<? extends ChangeList> changeLists) {
    List<String> changelistNames = ContainerUtil.map(changeLists, ChangeList::getName);
    Collections.sort(changelistNames);
    myExistingListsCombo.setModel(new DefaultComboBoxModel<>(ArrayUtil.toStringArray(changelistNames)));
  }

  public void setSuggestedName(@Nonnull String name) {
    if (StringUtil.isEmptyOrSpaces(name)) return;
    if (getExistingChangelistByName(name) != null) {
      myExistingListsCombo.setSelectedItem(name);
    }
    else {
      myNewNameSuggested = true;
      if (VcsConfiguration.getInstance(myProject).PRESELECT_EXISTING_CHANGELIST) {
        myExistingListsCombo.insertItemAt(name, 0);
        selectActiveChangeListIfExist();
      }
      else {
        myListPanel.setChangeListName(name);
      }
    }
    updateDescription();
  }

  private void selectActiveChangeListIfExist() {
    myExistingListsCombo.setSelectedItem(ChangeListManager.getInstance(myProject).getDefaultChangeList().getName());
  }

  public void updateEnabled() {
    if (myProject != null) {
      myListPanel.nameChangedImpl(myProject, null);
    }
  }

  /**
   * Method used as getResult, usually invoked inside doOkAction
   */
  @Nullable
  public LocalChangeList getSelectedList(Project project) {
    ChangeListManager manager = ChangeListManager.getInstance(project);
    String changeListName = myListPanel.getChangeListName();
    LocalChangeList localChangeList = manager.findChangeList(changeListName);

    if (localChangeList == null) {
      localChangeList = manager.addChangeList(changeListName, myListPanel.getDescription());
      myListPanel.changelistCreatedOrChanged(localChangeList);
    }
    else {
      //update description if changed
      localChangeList.setComment(myListPanel.getDescription());
    }
    rememberSettings(project, localChangeList.isDefault(), myListPanel.getMakeActiveCheckBox().isSelected());
    if (myListPanel.getMakeActiveCheckBox().isSelected()) {
      manager.setDefaultChangeList(localChangeList);
    }
    return localChangeList;
  }

  private void rememberSettings(@Nonnull Project project, boolean activeListSelected, boolean setActive) {
    if (myNewNameSuggested) {
      VcsConfiguration.getInstance(project).PRESELECT_EXISTING_CHANGELIST = activeListSelected;
    }
    VcsConfiguration.getInstance(project).MAKE_NEW_CHANGELIST_ACTIVE = setActive;
  }

  public void setDefaultSelection(final ChangeList defaultSelection) {
    if (defaultSelection == null) {
      selectActiveChangeListIfExist();
    }
    else {
      myExistingListsCombo.setSelectedItem(defaultSelection.getName());
    }
    updateDescription();
    updateEnabled();
  }

  private void updateDescription() {
    LocalChangeList list = getExistingChangelistByName(myListPanel.getChangeListName());
    String newText = list != null ? list.getComment() : myLastTypedDescription;
    if (!StringUtil.equals(myListPanel.getDescription(), newText)) {
      myListPanel.setDescription(newText);
    }
  }

  private LocalChangeList getExistingChangelistByName(@Nonnull String changeListName) {
    ChangeListManager manager = ChangeListManager.getInstance(myProject);
    return manager.findChangeList(changeListName);
  }

  public JComponent getPreferredFocusedComponent() {
    return myExistingListsCombo;
  }

  private static class MyEditorComboBox extends ComboBox<String> {

    private static final int PREF_WIDTH = 200;

    public MyEditorComboBox(Project project) {
      super(PREF_WIDTH);
      setEditor(new StringComboboxEditor(project, PlainTextFileType.INSTANCE, this) {
        @Override
        protected void onEditorCreate(EditorEx editor) {
          super.onEditorCreate(editor);
          getDocument().putUserData(CONTINUE_RUN_COMPLETION, true);
        }
      });
    }

    @Nonnull
    private EditorTextField getEditorTextField() {
      return ObjectUtils.assertNotNull((EditorTextField)getEditor().getEditorComponent());
    }
  }
}
