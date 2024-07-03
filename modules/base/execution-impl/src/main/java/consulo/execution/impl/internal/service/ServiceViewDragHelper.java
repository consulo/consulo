// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.dataContext.DataProvider;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.service.ServiceViewContributor;
import consulo.execution.service.ServiceViewDescriptor;
import consulo.execution.service.ServiceViewDnDDescriptor;
import consulo.execution.service.ServiceViewManager;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.project.Project;
import consulo.ui.ex.awt.RelativeRectangle;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.dnd.*;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import static consulo.execution.service.ServiceViewDnDDescriptor.Position.*;

final class ServiceViewDragHelper {
  static DnDSource createSource(@Nonnull ServiceView serviceView) {
    return new ServiceViewDnDSource(serviceView);
  }

  static DnDTarget createTarget(@Nonnull JTree tree) {
    return new ServiceViewDnDTarget(tree);
  }

  static void installDnDSupport(@Nonnull Project project,
                                @Nonnull ToolWindowInternalDecorator decorator,
                                @Nonnull ContentManager contentManager) {
    Content dropTargetContent = createDropTargetContent();
    JComponent awtDecorator = (JComponent)decorator;
    DnDSupport.createBuilder(awtDecorator)
              .setTargetChecker(event -> {
                Object o = event.getAttachedObject();
                boolean dropPossible =
                  o instanceof ServiceViewDragBean && event.getPointOn(awtDecorator).y < decorator.getHeaderHeight();
                event.setDropPossible(dropPossible, "");
                if (dropPossible) {
                  if (contentManager.getIndexOfContent(dropTargetContent) < 0) {
                    contentManager.addContent(dropTargetContent);
                  }

                  ServiceViewDragBean dragBean = (ServiceViewDragBean)o;
                  ItemPresentation presentation;
                  if (dragBean.getItems().size() > 1 && dragBean.getContributor() != null) {
                    presentation = dragBean.getContributor().getViewDescriptor(project).getPresentation();
                  }
                  else {
                    ServiceViewItem item = dragBean.getItems().get(0);
                    presentation = item.getViewDescriptor().getPresentation();
                    dropTargetContent.setTabColor(item.getColor());
                  }
                  dropTargetContent.setDisplayName(getDisplayName(presentation));
                  dropTargetContent.setIcon(presentation.getIcon(false));
                }
                else if (contentManager.getIndexOfContent(dropTargetContent) >= 0) {
                  contentManager.removeContent(dropTargetContent, false);
                }
                return true;
              })
              .setCleanUpOnLeaveCallback(() -> {
                if (!contentManager.isDisposed() && contentManager.getIndexOfContent(dropTargetContent) >= 0) {
                  contentManager.removeContent(dropTargetContent, false);
                }
              })
              .setDropHandler(new DnDDropHandler() {
                @Override
                public void drop(DnDEvent event) {
                  Object o = event.getAttachedObject();
                  if (o instanceof ServiceViewDragBean) {
                    ((ServiceViewManagerImpl)ServiceViewManager.getInstance(project)).extract((ServiceViewDragBean)o);
                  }
                }
              })
              .install();
    awtDecorator.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        if (contentManager.getIndexOfContent(dropTargetContent) >= 0) {
          contentManager.removeContent(dropTargetContent, false);
        }
      }
    });
  }

  static String getDisplayName(ItemPresentation presentation) {
    StringBuilder result = new StringBuilder();
    if (presentation instanceof PresentationData) {
      List<PresentableNodeDescriptor.ColoredFragment> fragments = ((PresentationData)presentation).getColoredText();
      if (fragments.isEmpty() && presentation.getPresentableText() != null) {
        result.append(presentation.getPresentableText());
      }
      else {
        for (PresentableNodeDescriptor.ColoredFragment fragment : fragments) {
          result.append(fragment.getText());
        }
      }
    }
    else if (presentation.getPresentableText() != null) {
      result.append(presentation.getPresentableText());
    }
    return result.toString();
  }

  @Nullable
  static ServiceViewContributor getTheOnlyRootContributor(List<? extends ServiceViewItem> items) {
    ServiceViewContributor result = null;
    for (ServiceViewItem node : items) {
      if (result == null) {
        result = node.getRootContributor();
      }
      else if (result != node.getRootContributor()) {
        return null;
      }
    }
    return result;
  }

  private static Content createDropTargetContent() {
    Content content = ContentFactory.getInstance().createContent(new JPanel(), null, false);
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    content.setCloseable(true);
    return content;
  }

  static final class ServiceViewDragBean implements DataProvider {
    private final ServiceView myServiceView;
    private final List<ServiceViewItem> myItems;
    private final ServiceViewContributor myContributor;

    ServiceViewDragBean(@Nonnull ServiceView serviceView, @Nonnull List<ServiceViewItem> items) {
      myServiceView = serviceView;
      myItems = ContainerUtil.filter(items, item -> {
        ServiceViewItem parent = item.getParent();
        while (parent != null) {
          if (items.contains(parent)) {
            return false;
          }
          parent = parent.getParent();
        }
        return true;
      });
      myContributor = getTheOnlyRootContributor(myItems);
    }

    @Nonnull
    ServiceView getServiceView() {
      return myServiceView;
    }

    @Nonnull
    List<ServiceViewItem> getItems() {
      return myItems;
    }

    @Nullable
    ServiceViewContributor getContributor() {
      return myContributor;
    }

    @Nullable
    @Override
    public Object getData(@Nonnull Key<?> dataId) {
      if (PlatformDataKeys.SELECTED_ITEMS.is(dataId)) {
        return ContainerUtil.map2Array(myItems, ServiceViewItem::getValue);
      }
      if (PlatformDataKeys.SELECTED_ITEM.is(dataId)) {
        ServiceViewItem item = ContainerUtil.getOnlyItem(myItems);
        return item != null ? item.getValue() : null;
      }
      return null;
    }
  }

  private static final class ServiceViewDnDSource implements DnDSource {
    private final ServiceView myServiceView;

    ServiceViewDnDSource(@Nonnull ServiceView serviceView) {
      myServiceView = serviceView;
    }

    @Override
    public boolean canStartDragging(DnDAction action, @Nonnull Point dragOrigin) {
      return !myServiceView.getSelectedItems().isEmpty();
    }

    @Override
    public DnDDragStartBean startDragging(DnDAction action, @Nonnull Point dragOrigin) {
      return new DnDDragStartBean(new ServiceViewDragBean(myServiceView, myServiceView.getSelectedItems()));
    }

    @Override
    public Pair<Image, Point> createDraggedImage(DnDAction action,
                                                 Point dragOrigin,
                                                 @Nonnull DnDDragStartBean bean) {
      ServiceViewDragBean dragBean = (ServiceViewDragBean)bean.getAttachedObject();
      int size = dragBean.getItems().size();
      ItemPresentation presentation = null;
      if (size == 1) {
        presentation = dragBean.getItems().get(0).getViewDescriptor().getPresentation();
      }
      else {
        ServiceViewContributor contributor = dragBean.getContributor();
        if (contributor != null) {
          presentation = contributor.getViewDescriptor(myServiceView.getProject()).getPresentation();
        }
      }

      SimpleColoredComponent c = new SimpleColoredComponent();
      c.setForeground(myServiceView.getForeground());
      c.setBackground(myServiceView.getBackground());
      if (presentation != null) {
        c.setIcon(presentation.getIcon(false));
        c.append(getDisplayName(presentation));
      }
      else {
        LocalizeValue text = ExecutionLocalize.serviceViewItems(size);
        c.append(text.get());
      }

      Dimension preferredSize = c.getPreferredSize();
      c.setSize(preferredSize);
      BufferedImage image = UIUtil.createImage(c, preferredSize.width, preferredSize.height, BufferedImage.TYPE_INT_ARGB);
      c.setOpaque(false);
      Graphics2D g = image.createGraphics();
      c.paint(g);
      g.dispose();
      return Pair.create(image, new Point(0, 0));
    }
  }

  private static final class ServiceViewDnDTarget implements DnDTarget {
    private final JTree myTree;

    ServiceViewDnDTarget(@Nonnull JTree tree) {
      myTree = tree;
    }

    @Override
    public void drop(DnDEvent event) {
      EventContext eventContext = getEventContext(event.getPoint());
      if (eventContext == null) return;

      if (eventContext.descriptor.canDrop(event, INTO)) {
        eventContext.descriptor.drop(event, INTO);
      }
      else {
        eventContext.descriptor.drop(event, eventContext.getPosition());
      }
      event.hideHighlighter();
    }

    @Override
    public boolean update(DnDEvent event) {
      event.setDropPossible(false);
      EventContext eventContext = getEventContext(event.getPoint());
      if (eventContext == null) return true;

      if (eventContext.descriptor.canDrop(event, INTO)) {
        event.setDropPossible(true);
        RelativeRectangle rectangle = new RelativeRectangle(myTree, eventContext.cellBounds);
        event.setHighlighting(rectangle, DnDEvent.DropTargetHighlightingType.RECTANGLE);
        return false;
      }

      ServiceViewDnDDescriptor.Position position = eventContext.getPosition();
      if (eventContext.descriptor.canDrop(event, position)) {
        event.setDropPossible(true);
        Rectangle bounds = eventContext.cellBounds;
        bounds.y -= -1;
        bounds.height = 2;
        if (position != ABOVE) {
          bounds.y += bounds.height;
        }
        RelativeRectangle rectangle = new RelativeRectangle(myTree, bounds);
        event.setHighlighting(rectangle, DnDEvent.DropTargetHighlightingType.FILLED_RECTANGLE);
        return false;
      }

      event.hideHighlighter();
      return false;
    }

    private EventContext getEventContext(Point point) {
      TreePath path = myTree.getPathForLocation(point.x, point.y);
      if (path == null || !(path.getLastPathComponent() instanceof ServiceViewItem item)) return null;

      Rectangle cellBounds = myTree.getPathBounds(path);
      if (cellBounds == null) return null;

      ServiceViewDescriptor viewDescriptor = item.getViewDescriptor();
      if (!(viewDescriptor instanceof ServiceViewDnDDescriptor)) return null;

      return new EventContext(point, cellBounds, (ServiceViewDnDDescriptor)viewDescriptor);
    }

    private static final class EventContext {
      final Point point;
      final Rectangle cellBounds;
      final ServiceViewDnDDescriptor descriptor;

      private EventContext(Point point, Rectangle cellBounds, ServiceViewDnDDescriptor descriptor) {
        this.point = point;
        this.cellBounds = cellBounds;
        this.descriptor = descriptor;
      }

      ServiceViewDnDDescriptor.Position getPosition() {
        return point.y < cellBounds.y + cellBounds.height / 2 ? ABOVE : BELOW;
      }
    }
  }
}
