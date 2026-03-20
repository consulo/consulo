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
package consulo.desktop.awt.internal.diff.util;

import consulo.diff.DiffContext;
import consulo.dataContext.DataProvider;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.ui.ex.awt.Wrapper;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class DiffPanelBase extends JPanel implements DataProvider {
  protected final @Nullable Project myProject;
  
  private final DataProvider myDataProvider;
  
  protected final DiffContext myContext;

  
  private final List<JComponent> myPersistentNotifications = new ArrayList<JComponent>();

  
  protected final JPanel myContentPanel;
  
  protected final JPanel myNotificationsPanel;

  
  private final Wrapper myNorthPanel;
  
  private final Wrapper mySouthPanel;

  
  protected final CardLayout myCardLayout;

  
  protected String myCurrentCard;

  public DiffPanelBase(@Nullable Project project,
                       DataProvider provider,
                       DiffContext context) {
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

  public void setTopPanel(@Nullable JComponent component) {
    myNorthPanel.setContent(component);
  }

  public void setBottomPanel(@Nullable JComponent component) {
    mySouthPanel.setContent(component);
  }

  protected void setCurrentCard(String card) {
    setCurrentCard(card, true);
  }

  protected void setCurrentCard(String card, boolean keepFocus) {
    boolean restoreFocus = keepFocus && myContext.isFocused();

    myCardLayout.show(myContentPanel, card);
    myCurrentCard = card;
    myContentPanel.revalidate();

    if (restoreFocus) myContext.requestFocus();
  }

  @Override
  public @Nullable Object getData(Key<?> dataId) {
    return myDataProvider.getData(dataId);
  }

  //
  // Notifications
  //

  public void setPersistentNotifications(List<JComponent> components) {
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

  public void addNotification(JComponent notification) {
    myNotificationsPanel.add(notification);
    myNotificationsPanel.revalidate();
  }
}
