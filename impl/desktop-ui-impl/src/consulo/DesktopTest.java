package consulo;/*
 * Copyright 2013-2016 must-be.org
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

import consulo.ui.Component;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class DesktopTest {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // swing api start
        JFrame main = new JFrame();
        main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        main.setSize(400, 320);
        main.setLocationRelativeTo(null);
        // swing api stop

        final Component component = SomeTestUIBuilder.build();

        // swing api start
        main.add((java.awt.Component)component);
        main.setVisible(true);
        // swing api stop
      }
    });
  }
}
