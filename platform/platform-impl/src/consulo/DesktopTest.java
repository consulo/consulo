/*
 * Copyright 2013-2016 consulo.io
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
package consulo;

import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Window;
import consulo.ui.shared.Size;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */

public class DesktopTest {
  public static class JFrameWrapper extends JFrame implements Window {

    @Override
    public void setSize(@NotNull Size size) {
      setSize(new Dimension(size.getWidth(), size.getHeight()));
    }

    @Override
    public void setContent(@NotNull Component content) {
      setContentPane((java.awt.Container)content);
    }

    @Override
    public void setMenuBar(@Nullable MenuBar menuBar) {
      setJMenuBar((JMenuBar)menuBar);
    }

    @Nullable
    @Override
    public Component getParentComponent() {
      return null;
    }
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
        catch (InstantiationException e) {
          e.printStackTrace();
        }
        catch (IllegalAccessException e) {
          e.printStackTrace();
        }
        catch (UnsupportedLookAndFeelException e) {
          e.printStackTrace();
        }

        // swing api start
        JFrameWrapper main = new JFrameWrapper();
        main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        main.setSize(400, 320);
        main.setLocationRelativeTo(null);
        // swing api stop

        SomeTestUIBuilder.build(main);

        // swing api start
        main.setVisible(true);
        // swing api stop
      }
    });
  }
}
