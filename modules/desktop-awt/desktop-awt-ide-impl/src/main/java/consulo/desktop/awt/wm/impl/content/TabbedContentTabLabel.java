// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.impl.content;

import consulo.application.AllIcons;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.ui.ex.content.TabbedContent;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class TabbedContentTabLabel extends ContentTabLabel {

  private final TabbedContent myContent;
  private Reference<JBPopup> myPopupReference = null;

  public TabbedContentTabLabel(@Nonnull TabbedContent content, @Nonnull TabContentLayout layout) {
    super(content, layout);
    myContent = content;
  }

  private boolean isPopupShown() {
    return (myPopupReference != null && myPopupReference.get() != null && myPopupReference.get().isVisible());
  }

  @Override
  protected void selectContent() {
    IdeEventQueue.getInstance().getPopupManager().closeAllPopups();
    super.selectContent();

    if (hasMultipleTabs()) {
      SelectContentTabStep step = new SelectContentTabStep(getContent());
      ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
      myPopupReference = new WeakReference<>(popup);
      popup.showUnderneathOf(this);
      popup.addListener(new JBPopupAdapter() {
        @Override
        public void onClosed(@Nonnull LightweightWindowEvent event) {
          repaint();
        }
      });
    }
  }

  @Override
  public void update() {
    super.update();
    if (myContent != null) {
      setText(myContent.getTabName());
    }
  }

  @Override
  protected void fillIcons(List<AdditionalIcon> icons) {
    icons.add(new AdditionalIcon(AllIcons.General.ArrowDown, AllIcons.General.ArrowDown) {
      @Nonnull
      @Override
      public Rectangle getRectangle() {
        return new Rectangle(getX(), 0, getIconWidth(), getHeight());
      }

      @Override
      public boolean isHovered() {
        return mouseOverIcon(this) || isPopupShown();
      }

      @Override
      public boolean getAvailable() {
        return hasMultipleTabs();
      }

      @Nullable
      @Override
      public Runnable getAction() {
        return () -> selectContent();
      }
    });
    super.fillIcons(icons);
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    JBPopup popup = SoftReference.dereference(myPopupReference);
    if (popup != null) {
      Disposer.dispose(popup);
      myPopupReference = null;
    }
  }

  @Nonnull
  @Override
  public TabbedContent getContent() {
    return myContent;
  }

  private boolean hasMultipleTabs() {
    return myContent != null && myContent.hasMultipleTabs();
  }
}
