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
package consulo.spash;

import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 28-Dec-16
 */
public class Test {
  public static void main(String[] args) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setSize(JBUI.size(602, 294));
    panel.setBackground(Color.LIGHT_GRAY);


    panel.add(new AnimatedLogoLabel(14, false), BorderLayout.SOUTH);


    JFrame frame = new JFrame();
    frame.setContentPane(panel);
    frame.setSize(panel.getSize());
    frame.setUndecorated(true);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}
