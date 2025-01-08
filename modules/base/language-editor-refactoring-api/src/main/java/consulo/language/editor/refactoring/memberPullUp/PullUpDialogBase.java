/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.memberPullUp;

import consulo.language.editor.refactoring.classMember.AbstractMemberInfoStorage;
import consulo.language.editor.refactoring.classMember.MemberInfoBase;
import consulo.language.editor.refactoring.classMember.MemberInfoChange;
import consulo.language.editor.refactoring.classMember.MemberInfoModel;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.ui.AbstractMemberSelectionTable;
import consulo.language.editor.refactoring.ui.MemberSelectionPanelBase;
import consulo.language.editor.refactoring.ui.RefactoringDialog;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.usage.UsageViewUtil;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Max Medvedev
 */
public abstract class PullUpDialogBase<Storage extends AbstractMemberInfoStorage<Member, Class, MemberInfo>,
        MemberInfo extends MemberInfoBase<Member>,
        Member extends PsiElement,
        Class extends PsiElement> extends RefactoringDialog {
  protected MemberSelectionPanelBase<Member, MemberInfo, AbstractMemberSelectionTable<Member, MemberInfo>> myMemberSelectionPanel;
  protected MemberInfoModel<Member, MemberInfo> myMemberInfoModel;
  protected final Class myClass;
  protected final List<Class> mySuperClasses;
  protected final Storage myMemberInfoStorage;
  protected List<MemberInfo> myMemberInfos;
  private JComboBox myClassCombo;

  public PullUpDialogBase(Project project, Class aClass, List<Class> superClasses, Storage memberInfoStorage, String title) {
    super(project, true);
    myClass = aClass;
    mySuperClasses = superClasses;
    myMemberInfoStorage = memberInfoStorage;
    myMemberInfos = myMemberInfoStorage.getClassMemberInfos(aClass);

    setTitle(title);
  }

  @Nullable
  public Class getSuperClass() {
    if (myClassCombo != null) {
      return (Class) myClassCombo.getSelectedItem();
    }
    else {
      return null;
    }
  }

  public List<MemberInfo> getSelectedMemberInfos() {
    ArrayList<MemberInfo> list = new ArrayList<MemberInfo>(myMemberInfos.size());
    for (MemberInfo info : myMemberInfos) {
      if (info.isChecked() && myMemberInfoModel.isMemberEnabled(info)) {
        list.add(info);
      }
    }
    return list;
  }

  @Override
  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel();

    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();

    gbConstraints.insets = new Insets(4, 0, 4, 8);
    gbConstraints.weighty = 1;
    gbConstraints.weightx = 1;
    gbConstraints.gridy = 0;
    gbConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.anchor = GridBagConstraints.WEST;
    final JLabel classComboLabel = new JLabel();
    panel.add(classComboLabel, gbConstraints);

    myClassCombo = new JComboBox(mySuperClasses.toArray());
    initClassCombo(myClassCombo);
    classComboLabel.setText(RefactoringLocalize.pullUpMembersTo(UsageViewUtil.getLongName(myClass)).get());
    classComboLabel.setLabelFor(myClassCombo);
    final Class preselection = getPreselection();
    int indexToSelect = 0;
    if (preselection != null) {
      indexToSelect = mySuperClasses.indexOf(preselection);
    }
    myClassCombo.setSelectedIndex(indexToSelect);
    myClassCombo.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        updateMemberInfo();
      }
    });
    updateMemberInfo();
    gbConstraints.gridy++;
    panel.add(myClassCombo, gbConstraints);

    return panel;
  }

  protected abstract void initClassCombo(JComboBox classCombo);

  protected abstract Class getPreselection();

  protected void updateMemberInfo() {
    final Class targetClass = (Class) myClassCombo.getSelectedItem();
    myMemberInfos = myMemberInfoStorage.getIntermediateMemberInfosList(targetClass);
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    myMemberSelectionPanel =
      new MemberSelectionPanelBase<>(RefactoringLocalize.membersToBePulledUp().get(), createMemberSelectionTable(myMemberInfos));
    myMemberInfoModel = createMemberInfoModel();
    myMemberInfoModel.memberInfoChanged(new MemberInfoChange<>(myMemberInfos));
    myMemberSelectionPanel.getTable().setMemberInfoModel(myMemberInfoModel);
    myMemberSelectionPanel.getTable().addMemberInfoChangeListener(myMemberInfoModel);
    panel.add(myMemberSelectionPanel, BorderLayout.CENTER);

    addCustomElementsToCentralPanel(panel);

    return panel;
  }

  protected void addCustomElementsToCentralPanel(JPanel panel) { }

  protected abstract AbstractMemberSelectionTable<Member,MemberInfo> createMemberSelectionTable(List<MemberInfo> infos);

  protected abstract MemberInfoModel<Member, MemberInfo> createMemberInfoModel();
}
