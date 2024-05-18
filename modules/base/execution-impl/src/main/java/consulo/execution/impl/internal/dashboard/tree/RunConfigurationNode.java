// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard.tree;

import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.dashboard.RunDashboardCustomizer;
import consulo.execution.dashboard.RunDashboardManager;
import consulo.execution.dashboard.RunDashboardRunConfigurationNode;
import consulo.execution.dashboard.RunDashboardRunConfigurationStatus;
import consulo.execution.executor.Executor;
import consulo.execution.impl.internal.ui.RunContentManagerImpl;
import consulo.execution.internal.RunManagerEx;
import consulo.execution.ui.RunContentDescriptor;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author konstantin.aleev
 */
public final class RunConfigurationNode extends AbstractTreeNode<RunDashboardManager.RunDashboardService>
  implements RunDashboardRunConfigurationNode {

  private final List<RunDashboardCustomizer> myCustomizers;
  private final UserDataHolder myUserDataHolder = new UserDataHolderBase();

  public RunConfigurationNode(Project project, @Nonnull RunDashboardManager.RunDashboardService service,
                              @Nonnull List<RunDashboardCustomizer> customizers) {
    super(project, service);
    myCustomizers = customizers;
  }

  @Override
  @Nonnull
  public RunnerAndConfigurationSettings getConfigurationSettings() {
    //noinspection ConstantConditions ???
    return getValue().getSettings();
  }

  @Nullable
  @Override
  public RunContentDescriptor getDescriptor() {
    //noinspection ConstantConditions ???
    return getValue().getDescriptor();
  }

  @Nullable
  @Override
  public Content getContent() {
    //noinspection ConstantConditions ???
    return getValue().getContent();
  }

  @Override
  protected void update(@Nonnull PresentationData presentation) {
    RunnerAndConfigurationSettings configurationSettings = getConfigurationSettings();
    //noinspection ConstantConditions
    boolean isStored = RunManager.getInstance(getProject()).hasSettings(configurationSettings);
    SimpleTextAttributes nameAttributes;
    if (isStored) {
      nameAttributes = getContent() != null ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }
    else {
      nameAttributes = SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES;
    }
    presentation.addText(configurationSettings.getName(), nameAttributes);
    Image icon = getIcon(configurationSettings);
    presentation.setIcon(isStored ? icon : ImageEffects.grayed(icon));

    for (RunDashboardCustomizer customizer : myCustomizers) {
      if (customizer.updatePresentation(presentation, this)) {
        return;
      }
    }
  }

  private Image getIcon(RunnerAndConfigurationSettings configurationSettings) {
    Image icon = null;
    RunDashboardRunConfigurationStatus status = getStatus();
    if (RunDashboardRunConfigurationStatus.STARTED.equals(status)) {
      icon = getExecutorIcon();
    }
    else if (RunDashboardRunConfigurationStatus.FAILED.equals(status)) {
      icon = status.getIcon();
    }
    if (icon == null) {
      icon = RunManagerEx.getInstanceEx(getProject()).getConfigurationIcon(configurationSettings);
    }
    return icon;
  }

  @Nonnull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    for (RunDashboardCustomizer customizer : myCustomizers) {
      Collection<? extends AbstractTreeNode<?>> children = customizer.getChildren(this);
      if (children != null) {
        for (AbstractTreeNode<?> child : children) {
          child.setParent(this);
        }
        return children;
      }
    }
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return myUserDataHolder.getUserData(key);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    myUserDataHolder.putUserData(key, value);
  }

  @Nonnull
  @Override
  public List<RunDashboardCustomizer> getCustomizers() {
    return myCustomizers;
  }

  @Nonnull
  @Override
  public RunDashboardRunConfigurationStatus getStatus() {
    for (RunDashboardCustomizer customizer : myCustomizers) {
      RunDashboardRunConfigurationStatus status = customizer.getStatus(this);
      if (status != null) {
        return status;
      }
    }
    return RunDashboardRunConfigurationStatus.getStatus(this);
  }

  @Nullable
  private Image getExecutorIcon() {
    Content content = getContent();
    if (content != null) {
      if (!RunContentManagerImpl.isTerminated(content)) {
        Executor executor = RunContentManagerImpl.getExecutorByContent(content);
        if (executor != null) {
          return executor.getIcon();
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return getConfigurationSettings().getName();
  }
}
