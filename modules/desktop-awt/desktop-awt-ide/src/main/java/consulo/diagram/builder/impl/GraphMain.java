/*
 * Copyright 2013-2018 consulo.io
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
package consulo.diagram.builder.impl;

import com.intellij.icons.AllIcons;
import consulo.diagram.builder.GraphBuilder;
import consulo.diagram.builder.GraphBuilderFactory;
import consulo.diagram.builder.GraphNode;
import consulo.diagram.builder.GraphPositionStrategy;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 22:48/15.10.13
 */
public class GraphMain extends JFrame {
  public GraphMain() {
    super("Graph Test");

    GraphBuilderFactory graphBuilderFactory = new DesktopGraphBuilderFactoryImpl();

    GraphBuilder builder = graphBuilderFactory.createBuilder();

    GraphNode<?> testNode1 = builder.createNode("Test Node1", AllIcons.Nodes.Class, null, GraphPositionStrategy.CENTER);
    GraphNode<?> testNode2 = builder.createNode("Test Node2", AllIcons.Nodes.Class, null, GraphPositionStrategy.BOTTOM);

    testNode1.makeArrow(testNode2);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(builder.getComponent(), BorderLayout.CENTER);
    setContentPane(panel);
  }

  public static void main(String[] args) {
    GraphMain main = new GraphMain();
    main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    main.setLocationByPlatform(false);
    main.setLocationRelativeTo(null);
    main.setSize(400, 320);
    main.setVisible(true);
  }
}