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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;

/**
 * @author VISTALL
 * @since 11/12/2021
 */
public class DesktopSwtProgressBarImpl extends SWTComponentDelegate<ProgressBar> implements consulo.ui.ProgressBar {
  @Override
  protected ProgressBar createSWT(Composite parent) {
    return new ProgressBar(parent, SWT.DEFAULT);
  }

  @Override
  public void setIndeterminate(boolean value) {
    // SWT.INDETERMINATE - todo not supported change
  }

  @Override
  public boolean isIndeterminate() {
    return false;
  }

  @Override
  public void setMinimum(int value) {
   toSWTComponent().setMinimum(value);
  }

  @Override
  public void setMaximum(int value) {
    toSWTComponent().setMaximum(value);
  }

  @Override
  public void setValue(int value) {
    toSWTComponent().setSelection(value);
  }
}
