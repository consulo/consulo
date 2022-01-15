package com.intellij.coverage;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullComputable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.options.SimpleConfigurable;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.ValueGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * User: anna
 * Date: 12/16/10
 */
public class CoverageOptionsConfigurable extends SimpleConfigurable<CoverageOptionsConfigurable.Panel> implements SearchableConfigurable, Configurable.NoScroll {
  public static class Panel implements NotNullComputable<Component> {
    private final Disposable myUiDisposable;

    private RadioButton myShowOptionsRB;
    private RadioButton myReplaceRB;
    private RadioButton myAddRB;
    private RadioButton myDoNotApplyRB;

    private CheckBox myActivateCoverageViewCB;

    private VerticalLayout myWholePanel;

    private List<Configurable> myChildren = new ArrayList<>();

    @RequiredUIAccess
    Panel(Project project, Disposable uiDisposable) {
      myUiDisposable = uiDisposable;
      
      myWholePanel = VerticalLayout.create();

      ValueGroup<Boolean> group = ValueGroup.createBool();
      VerticalLayout primaryGroup = VerticalLayout.create();

      myShowOptionsRB = RadioButton.create(LocalizeValue.localizeTODO("Show options before applying coverage to the editor"));
      primaryGroup.add(myShowOptionsRB);
      group.add(myShowOptionsRB);

      myDoNotApplyRB = RadioButton.create(LocalizeValue.localizeTODO("Do not apply collected coverage"));
      primaryGroup.add(myDoNotApplyRB);
      group.add(myDoNotApplyRB);

      myReplaceRB = RadioButton.create(LocalizeValue.localizeTODO("Replace active suites with the new one"));
      primaryGroup.add(myReplaceRB);
      group.add(myReplaceRB);

      myAddRB = RadioButton.create(LocalizeValue.localizeTODO("Add to the active suites"));
      primaryGroup.add(myAddRB);
      group.add(myAddRB);
      
      myActivateCoverageViewCB = CheckBox.create(LocalizeValue.localizeTODO("Activate Coverage &View "));
      primaryGroup.add(myActivateCoverageViewCB);
      
      myWholePanel.add(LabeledLayout.create(LocalizeValue.localizeTODO("When new coverage is gathered"), primaryGroup));

      for (CoverageOptions options : CoverageOptions.EP_NAME.getExtensionList(project)) {
        Configurable configurable = options.createConfigurable();
        if(configurable != null) {
          myChildren.add(configurable);
        }
      }

      for (Configurable child : myChildren) {
        Component uiComponent = child.createUIComponent(myUiDisposable);
        assert uiComponent != null;
        myWholePanel.add(LabeledLayout.create(LocalizeValue.of(child.getDisplayName()), uiComponent));
      }
    }

    @Nonnull
    @Override
    public Component compute() {
      return myWholePanel;
    }
  }

  private final CoverageOptionsProvider myManager;
  private Project myProject;

  @Inject
  public CoverageOptionsConfigurable(CoverageOptionsProvider manager, Project project) {
    myManager = manager;
    myProject = project;
  }

  @Nonnull
  @Override
  public String getId() {
    return "coverage";
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Coverage";
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Panel createPanel(@Nonnull Disposable uiDisposable) {
    return new Panel(myProject, uiDisposable);
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull Panel panel) throws ConfigurationException {
    myManager.setOptionsToReplace(getSelectedValue(panel));
    myManager.setActivateViewOnRun(panel.myActivateCoverageViewCB.getValueOrError());

    for (Configurable child : panel.myChildren) {
      child.apply();
    }
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull Panel panel) {
    final int addOrReplace = myManager.getOptionToReplace();
    switch (addOrReplace) {
      case 0:
        panel.myReplaceRB.setValue(true);
        break;
      case 1:
        panel.myAddRB.setValue(true);
        break;
      case 2:
        panel.myDoNotApplyRB.setValue(true);
        break;
      default:
        panel.myShowOptionsRB.setValue(true);
    }

    panel.myActivateCoverageViewCB.setValue(myManager.activateViewOnRun());

    for (Configurable child : panel.myChildren) {
      child.reset();
    }
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull Panel panel) {
    if (myManager.getOptionToReplace() != getSelectedValue(panel)) {
      return true;
    }

    if (myManager.activateViewOnRun() != panel.myActivateCoverageViewCB.getValueOrError()) {
      return true;
    }

    for (Configurable child : panel.myChildren) {
      if(child.isModified()) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  protected void disposeUIResources(@Nonnull Panel panel) {
    for (Configurable child : panel.myChildren) {
      child.disposeUIResources();
    }
    super.disposeUIResources(panel);
  }

  private int getSelectedValue(Panel panel) {
    if (panel.myReplaceRB.getValueOrError()) {
      return 0;
    }
    else if (panel.myAddRB.getValueOrError()) {
      return 1;
    }
    else if (panel.myDoNotApplyRB.getValueOrError()) {
      return 2;
    }
    return 3;
  }
}
