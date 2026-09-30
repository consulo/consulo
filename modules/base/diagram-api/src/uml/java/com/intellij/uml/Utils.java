/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.uml;

import com.intellij.openapi.graph.base.Edge;
import com.intellij.openapi.graph.base.Node;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.actions.layout.AbstractLayoutAction;
import com.intellij.openapi.graph.layout.Layouter;
import com.intellij.openapi.graph.layout.NodeLayout;
import com.intellij.openapi.graph.settings.GraphSettings;
import com.intellij.openapi.graph.settings.GraphSettingsProvider;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.graph.view.NodeRealizer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.uml.settings.UmlLayout;
import com.intellij.util.ui.UIUtil;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class Utils {
  private static Key<GraphBuilder<UmlNode, UmlEdge>> GRAPH_BUILDER = Key.create("UML_GRAPH_BUILDER");
  private static Key<UmlProvider> UML_PROVIDER = Key.create("UML_PROVIDER");

  public static GraphBuilder<UmlNode, UmlEdge> getBuilder(UmlDataModel<?> model) {
    return model == null ? null : model.getUserData(GRAPH_BUILDER);
  }

  public static void setGraphBuilder(GraphBuilder<UmlNode, UmlEdge> builder, UmlDataModel<?> model) {
    model.putUserData(GRAPH_BUILDER, builder);
  }

  public static UmlProvider getProvider(GraphBuilder<UmlNode, UmlEdge> builder) {
    return builder == null ? null : builder.getUserData(UML_PROVIDER);
  }

  public static void setProvider(GraphBuilder<UmlNode, UmlEdge> builder, UmlProvider provider) {
    builder.putUserData(UML_PROVIDER, provider);
  }

  public static Layouter getLayouter(Graph2D graph, Project project, UmlLayout layout) {
    final GraphSettings settings = GraphSettingsProvider.getInstance(project).getSettings(graph);
    switch (layout) {
      case BALLOON:
        return settings.getBalloonLayouter();
      case CIRCULAR:
        return settings.getCircularLayouter();
      case DIRECTED_ORTHOGONAL:
        return settings.getDirectedOrthogonalLayouter();
      case HIERARCHIC_GROUP:
        return settings.getGroupLayouter();
      case ORGANIC:
        return settings.getOrganicLayouter();
      case ORTHOGONAL:
        return settings.getOrthogonalLayouter();
      default:
        return settings.getGroupLayouter();
    }
  }

  public static UmlDataModel<?> getDataModel(GraphBuilder<UmlNode, UmlEdge> builder) {
    return ((UmlDataModelWrapper)builder.getGraphDataModel()).getModel();
  }

  public static boolean isEqual(final String s1, final String s2) {
    return s1 == null && s2 == null || s1 != null && s1.equals(s2);
  }

  public static void updateGraph(final GraphBuilder<UmlNode, UmlEdge> myBuilder) {
    updateGraph(myBuilder, true, false);
  }

  public static void updateGraph(final GraphBuilder<UmlNode, UmlEdge> myBuilder, final boolean increaseModTrackerCounter, final boolean updateLayout) {
    if (increaseModTrackerCounter) {
      ((Updateable)myBuilder.getGraphPresentationModel()).update();
      for (UmlEdge umlEdge : myBuilder.getEdgeObjects()) {
        final Edge edge = myBuilder.getEdge(umlEdge);
        myBuilder.getGraph().getRealizer(edge).clearBends();
      }
    }

    UIUtil.invokeLaterIfNeeded(new Runnable() {
      public void run() {
        myBuilder.updateGraph();
        if (updateLayout) {
          GraphSettings graphSettings = GraphSettingsProvider.getInstance(myBuilder.getProject()).getSettings(myBuilder.getGraph());
          AbstractLayoutAction.doLayout(myBuilder.getView(), graphSettings.getCurrentLayouter(), myBuilder.getProject());
        }
      }
    });
  }


  //public static UmlPresentationModel getPresentationModel(@NotNull GraphBuilder builder) {
  //  return (UmlPresentationModel)builder.getGraphPresentationModel();
  //}
  //
  //public static UmlPresentationSettings getPresentation(@NotNull GraphBuilder builder) {
  //  return getPresentationModel(builder).getPresentation();
  //}

  public static boolean hasNotNull(Object... objects) {
    for (Object object : objects) {
      if (object != null) return true;
    }
    return false;
  }

  public static Point getNodeCoordinatesOnScreen(Node node, Graph2DView view) {
    final Graph2D graph2D = (Graph2D)node.getGraph();
    final Point viewPoint = ((Graph2DView)graph2D.getCurrentView()).getViewPoint();

    final NodeRealizer nodeRealizer = graph2D.getRealizer(node);
    final double x = nodeRealizer.getX();
    final double y = nodeRealizer.getY();
    final JComponent owner = view.getCanvasComponent();
    final double oX = owner.getLocationOnScreen().getX();
    final double oY = owner.getLocationOnScreen().getY();

    double pX = (x - viewPoint.x) * view.getZoom() + oX;
    double pY = (y - viewPoint.y) * view.getZoom() + oY;
    pX = pX < oX ? oX : pX;
    //pX *= view.getZoom();
    //pY *= view.getZoom();
    return new Point((int)pX, (int)pY);
  }

  public static Point getBestPositionForNode(GraphBuilder builder) {
    double maxY, maxX, rightest, leftest, xx, yy;
    maxY = maxX = rightest = xx = yy = -Double.MAX_VALUE;
    leftest = Double.MAX_VALUE;

    for (Object umlNode : builder.getNodeObjects()) {
      final Node node = builder.getNode(umlNode);
      if (node != null) {
        final NodeLayout nodeLayout = builder.getGraph().getNodeLayout(node);
        if (nodeLayout != null) {
          final double w = nodeLayout.getWidth();
          final double nx = nodeLayout.getX();
          final double x = nx + w;
          final double h = nodeLayout.getHeight();
          final double ny = nodeLayout.getY();
          final double y = ny + h;

          if (x > rightest) rightest = x;
          if (x < leftest) leftest = x - w;

          if (y >= maxY) {
            xx = y == maxY ? Math.max(xx, nx) : nx;
            yy = Math.max(yy, ny);
            maxX = y == maxY ? Math.max(x, maxX) : x;
            maxY = y;
          }
        }
      }
    }
    if (maxY == -Double.MAX_VALUE || maxX == -Double.MAX_VALUE) {
      return new Point(200, 200);
    }
    else {
      Point p = new Point();
      if (rightest - maxX < 100) {
        p.setLocation(leftest, maxY + 20);
      }
      else {
        p.setLocation(maxX + 20, yy);
      }
      return p;
    }
  }


  public static Document readUmlFileFromFile(InputStream is) throws JDOMException, IOException {
    return new SAXBuilder().build(is);
  }

  public static Object[] getNodeElementsForCategory(UmlProvider umlProvider, Object parent, UmlCategory category) {
    List<Object> result = new ArrayList<Object>();
    if (parent != null && category != null) {
      final UmlNodeContentManager contentManager = umlProvider.getNodeContentManager();
      for (Object element : umlProvider.getElementManager().getNodeElements(parent)) {
        if (contentManager.isInCategory(element, category)) {
          result.add(element);
        }
      }
    }
    return result.toArray(new Object[result.size()]);
  }

  public static boolean isPopupMode(GraphBuilder<UmlNode, UmlEdge> builder) {
    return builder.getUserData(UmlDataKeys.UML_POPUP) != null;
  }

  public static UmlElementsProvider[] getElementProviders(UmlProvider provider) {
    if (provider == null || provider.getExtras() == null) return UmlElementsProvider.EMPTY_ARRAY;
    final UmlElementsProvider[] providers = provider.getExtras().getElementsProviders();
    return providers == null || providers.length == 0 ? UmlElementsProvider.EMPTY_ARRAY : providers;    
  }

  public static boolean isNewUml(GraphBuilder builder) {
    return getProvider(builder) != null;
  }
}