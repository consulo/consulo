/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.openapi.wm.impl.IdeGlassPaneImpl;
import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingWindowWrapper;
import consulo.ui.Component;
import consulo.ui.Size;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.window.WindowOverAWTWindow;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2020-05-29
 */
public class DesktopWindowWrapper extends WindowOverAWTWindow {
  private static class MyJDialog extends JDialog implements FromSwingWindowWrapper {
    private Window myWindow;

    private MyJDialog(Frame owner, String title) {
      super(owner, title);
    }

    @Nullable
    @Override
    public Window toUIWindow() {
      return Objects.requireNonNull(myWindow);
    }
  }

  public DesktopWindowWrapper(String title, WindowOptions options) {
    super(new MyJDialog((Frame)TargetAWT.to(options.getOwner()), title));

    MyJDialog dialog = (MyJDialog)toAWTWindow();
    dialog.setGlassPane(new IdeGlassPaneImpl(dialog.getRootPane(), false));
    dialog.myWindow = this;
  }

  @RequiredUIAccess
  @Override
  public void setTitle(@Nonnull String title) {
    JDialog dialog = (JDialog)toAWTWindow();
    dialog.setTitle(title);
  }

  @RequiredUIAccess
  @Override
  public void setContent(@Nonnull Component content) {
    JDialog dialog = (JDialog)toAWTWindow();

    dialog.setContentPane((java.awt.Container)TargetAWT.to(content));
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    toAWTWindow().setSize(TargetAWT.to(size));
  }
}
