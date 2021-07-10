/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtDockLayoutImpl extends DesktopSwtComponent<Composite> implements DockLayout {

  private DesktopSwtComponent myLeftComponent;
  private DesktopSwtComponent myCenterComponent;
  private DesktopSwtComponent myRightComponent;
  private DesktopSwtComponent myBottomComponent;
  private DesktopSwtComponent myTopComponent;

  private Composite myCenterComposite;

  @Override
  protected Composite createSWT(Composite parent) {
    return new Composite(parent, SWT.NONE);
  }

  @Override
  protected void initialize(Composite component) {
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 1;
    gridLayout.horizontalSpacing = 0;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    gridLayout.verticalSpacing = 0;
    component.setLayout(gridLayout);

    // top
    GridData topData = new GridData(GridData.FILL_HORIZONTAL);
    //topData.heightHint = 30;

    Composite top = new Composite(component, SWT.NONE);
    top.setLayout(new FillLayout());
    top.setLayoutData(topData);
    if (myTopComponent != null) {
      ((DesktopSwtComponent)myTopComponent).bind(top, null);
    }

    // center
    Composite centerContainer = new Composite(component, SWT.NONE);
    centerContainer.setLayout(new GridLayout(3, false));
    GridData cLayoutData = new GridData(GridData.FILL_BOTH);
    cLayoutData.grabExcessVerticalSpace = true;
    centerContainer.setLayoutData(cLayoutData);


    Composite left = new Composite(centerContainer, SWT.NONE);
    left.setLayout(new FillLayout());
    left.setLayoutData(new GridData(GridData.FILL_VERTICAL));

    if(myLeftComponent != null) {
      ((DesktopSwtComponent)myLeftComponent).bind(left, null);
    }

    myCenterComposite = new Composite(centerContainer, SWT.NONE);
    myCenterComposite.setLayout(new FillLayout());
    myCenterComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

    if(myCenterComponent != null) {
      ((DesktopSwtComponent)myCenterComponent).bind(myCenterComposite, null);
    }

    Composite right = new Composite(centerContainer, SWT.NONE);
    right.setLayout(new FillLayout());
    right.setLayoutData(new GridData(GridData.FILL_VERTICAL));


    if (myRightComponent != null) {
      ((DesktopSwtComponent)myRightComponent).bind(right, null);
    }

    // bottom
    GridData bottomData = new GridData(GridData.FILL_HORIZONTAL);
    //bottomData.heightHint = 30;

    Composite bottom = new Composite(component, SWT.NONE);
    bottom.setLayout(new FillLayout());
    bottom.setLayoutData(bottomData);

    if (myBottomComponent != null) {
      ((DesktopSwtComponent)myBottomComponent).bind(bottom, null);
    }
  }

  @Override
  public void disposeSWT() {
    super.disposeSWT();

    if(myCenterComponent != null) {
      myCenterComponent.disposeSWT();
    }
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout top(@Nonnull Component component) {
    myTopComponent = (DesktopSwtComponent)component;
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout bottom(@Nonnull Component component) {
    myBottomComponent = (DesktopSwtComponent)component;
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout center(@Nonnull Component component) {
    DesktopSwtComponent old = myCenterComponent;
    if(old != null) {
      old.setParent(null);
    }
    myCenterComponent = (DesktopSwtComponent)component;
    if(myCenterComposite != null) {
      myCenterComponent.bind(myCenterComposite, null);
      myCenterComposite.layout(true, true);
    }
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout left(@Nonnull Component component) {
    myLeftComponent = (DesktopSwtComponent)component;
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout right(@Nonnull Component component) {
    myRightComponent = (DesktopSwtComponent)component;
    return this;
  }
}
