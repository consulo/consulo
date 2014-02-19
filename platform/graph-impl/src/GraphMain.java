/*
 * Copyright 2013 must-be.org
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

import org.consulo.diagram.builder.GraphBuilder;
import org.consulo.diagram.builder.GraphBuilderFactory;
import org.consulo.diagram.builder.GraphNode;
import org.consulo.diagram.builder.GraphPositionStrategy;
import org.consulo.diagram.builder.impl.GraphBuilderFactoryImpl;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 22:48/15.10.13
 */
public class GraphMain extends JFrame {
  public GraphMain() {
    super("Graph Test");

    GraphBuilderFactory graphBuilderFactory = new GraphBuilderFactoryImpl();

    GraphBuilder builder = graphBuilderFactory.createBuilder();

    GraphNode<?> testNode1 = builder.createNode("TestNode1", GraphPositionStrategy.CENTER);
    GraphNode<?> testNode2 = builder.createNode("TestNode2", GraphPositionStrategy.BOTTOM);

    testNode1.makeArrow(testNode2);

    getContentPane().add(builder.getComponent());
  }

  public static void main(String[] args) {
    GraphMain main = new GraphMain();
    main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    main.setSize(400, 320);
    main.setVisible(true);
  }
}
