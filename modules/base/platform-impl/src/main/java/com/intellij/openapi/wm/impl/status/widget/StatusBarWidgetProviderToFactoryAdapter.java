// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.status.widget;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import consulo.disposer.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.*;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Objects;

public final class StatusBarWidgetProviderToFactoryAdapter implements StatusBarWidgetFactory {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final StatusBarWidgetProvider myProvider;

  private boolean widgetWasCreated;
  @Nullable
  private StatusBarWidget myWidget;

  public StatusBarWidgetProviderToFactoryAdapter(@Nonnull Project project, @Nonnull StatusBarWidgetProvider provider) {
    myProject = project;
    myProvider = provider;
  }

  @Override
  public
  @Nonnull
  String getId() {
    StatusBarWidget widget = getWidget();
    return widget != null ? widget.ID() : myProvider.getClass().getName();
  }

  @Nls
  @Override
  public
  @Nonnull
  String getDisplayName() {
    StatusBarWidget widget = getWidget();
    if (widget != null) {
      StatusBarWidget.WidgetPresentation presentation = widget.getPresentation();
      String result = presentation != null ? StringUtil.notNullize(presentation.getTooltipText()) : "";
      if (!result.isEmpty()) {
        return result;
      }
      if (ApplicationManager.getApplication().isInternal()) {
        //noinspection HardCodedStringLiteral
        return widget.ID();
      }
    }
    return "";
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    IdeFrame frame = WindowManager.getInstance().getIdeFrame(myProject);
    if (frame == null || !myProvider.isCompatibleWith(frame)) {
      return false;
    }
    return getWidget() != null;
  }

  @Override
  public boolean isConfigurable() {
    return !getDisplayName().isEmpty();
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return true;
  }

  @Override
  public
  @Nonnull
  StatusBarWidget createWidget(@Nonnull Project project) {
    return Objects.requireNonNull(getWidget());
  }

  @Nullable
  private StatusBarWidget getWidget() {
    if (!widgetWasCreated) {
      myWidget = myProvider.getWidget(myProject);
      widgetWasCreated = true;
    }

    return myWidget;
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {
    myWidget = null;
    widgetWasCreated = false;
    Disposer.dispose(widget);
  }

  public
  @Nonnull
  String getAnchor() {
    return myProvider.getAnchor();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StatusBarWidgetProviderToFactoryAdapter adapter = (StatusBarWidgetProviderToFactoryAdapter)o;
    return myProvider.equals(adapter.myProvider) && myProject.equals(adapter.myProject);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myProvider, myProject);
  }
}
