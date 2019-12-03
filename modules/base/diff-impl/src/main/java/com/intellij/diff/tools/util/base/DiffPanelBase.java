/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.tools.util.base;

import com.intellij.diff.DiffContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.ui.components.panels.Wrapper;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class DiffPanelBase extends JPanel implements DataProvider {
  @Nullable protected final Project myProject;
  @Nonnull
  private final DataProvider myDataProvider;
  @Nonnull
  protected final DiffContext myContext;

  @Nonnull
  private final List<JComponent> myPersistentNotifications = new ArrayList<JComponent>();

  @Nonnull
  protected final JPanel myContentPanel;
  @Nonnull
  protected final JPanel myNotificationsPanel;

  @Nonnull
  private final Wrapper myNorthPanel;
  @Nonnull
  private final Wrapper mySouthPanel;

  @Nonnull
  protected final CardLayout myCardLayout;

  @Nonnull
  protected String myCurrentCard;

  public DiffPanelBase(@Nullable Project project,
                       @Nonnull DataProvider provider,
                       @Nonnull DiffContext context) {
    super(new BorderLayout());
    myProject = project;
    myDataProvider = provider;
    myContext = context;

    myCardLayout = new CardLayout();
    myContentPanel = new JPanel(myCardLayout);

    myNotificationsPanel = new JPanel();
    myNotificationsPanel.setLayout(new BoxLayout(myNotificationsPanel, BoxLayout.Y_AXIS));

    myNorthPanel = new Wrapper();
    mySouthPanel = new Wrapper();

    add(myContentPanel, BorderLayout.CENTER);
    add(myNorthPanel, BorderLayout.NORTH);
    add(mySouthPanel, BorderLayout.SOUTH);
  }

  public void setTopPanel(@javax.annotation.Nullable JComponent component) {
    myNorthPanel.setContent(component);
  }

  public void setBottomPanel(@Nullable JComponent component) {
    mySouthPanel.setContent(component);
  }

  protected void setCurrentCard(@Nonnull String card) {
    setCurrentCard(card, true);
  }

  protected void setCurrentCard(@Nonnull String card, boolean keepFocus) {
    boolean restoreFocus = keepFocus && myContext.isFocused();

    myCardLayout.show(myContentPanel, card);
    myCurrentCard = card;
    myContentPanel.revalidate();

    if (restoreFocus) myContext.requestFocus();
  }

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    return myDataProvider.getData(dataId);
  }

  //
  // Notifications
  //

  public void setPersistentNotifications(@Nonnull List<JComponent> components) {
    for (JComponent notification : myPersistentNotifications) {
      myNotificationsPanel.remove(notification);
    }

    myPersistentNotifications.clear();
    myPersistentNotifications.addAll(components);

    for (JComponent notification : myPersistentNotifications) {
      myNotificationsPanel.add(notification);
    }
    myNotificationsPanel.revalidate();
  }

  public void resetNotifications() {
    myNotificationsPanel.removeAll();
    for (JComponent notification : myPersistentNotifications) {
      myNotificationsPanel.add(notification);
    }
    myNotificationsPanel.revalidate();
  }

  public void addNotification(@Nonnull JComponent notification) {
    myNotificationsPanel.add(notification);
    myNotificationsPanel.revalidate();
  }
}
