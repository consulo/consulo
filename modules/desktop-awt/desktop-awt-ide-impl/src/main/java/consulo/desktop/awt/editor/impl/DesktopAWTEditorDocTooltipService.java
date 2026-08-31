/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.editor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.internal.EditorDocTooltip;
import consulo.language.editor.ui.internal.EditorDocTooltipService;
import consulo.language.editor.ui.internal.HintManagerEx;
import consulo.ui.Point2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.util.lang.ref.Ref;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.function.Consumer;

@Singleton
@ServiceImpl
public class DesktopAWTEditorDocTooltipService implements EditorDocTooltipService {
    @Override
    @RequiredUIAccess
    public @Nullable EditorDocTooltip show(Editor editor, int offset, String html, @Nullable Consumer<String> linkActivated) {
        if (editor.isDisposed()) {
            return null;
        }

        HyperlinkListener hyperlinkListener = linkActivated == null ? null : new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    linkActivated.accept(e.getDescription());
                }
            }
        };

        Ref<Consumer<? super String>> newTextConsumerRef = new Ref<>();
        JComponent component = HintUtil.createInformationLabel(html, hyperlinkListener, null, newTextConsumerRef);
        component.setBorder(JBUI.Borders.empty(6, 6, 5, 6));

        LightweightHintImpl hint = new LightweightHintImpl(wrapInScrollPaneIfNeeded(component, editor));

        DocTooltip tooltip = new DocTooltip(hint, component, newTextConsumerRef.get(), editor, offset);
        tooltip.show();
        return tooltip;
    }

    @Override
    public void hideAllHints() {
        HintManager.getInstance().hideAllHints();
    }

    private static JComponent wrapInScrollPaneIfNeeded(JComponent component, Editor editor) {
        if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
            Dimension preferredSize = component.getPreferredSize();
            Dimension maxSize = getMaxPopupSize(editor);
            if (preferredSize.width > maxSize.width || preferredSize.height > maxSize.height) {
                // We expect documentation providers to exercise good judgement in limiting the displayed information,
                // but in any case, we don't want the hint to cover the whole screen, so we also implement certain limiting here.
                JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(component, true);
                scrollPane.setPreferredSize(new Dimension(Math.min(preferredSize.width, maxSize.width), Math.min(preferredSize.height, maxSize.height)));
                return scrollPane;
            }
        }
        return component;
    }

    private static Dimension getMaxPopupSize(Editor editor) {
        Rectangle rectangle = ScreenUtil.getScreenRectangle(editor.getContentComponent());
        return new Dimension((int) (0.9 * Math.max(640, rectangle.width)), (int) (0.33 * Math.max(480, rectangle.height)));
    }

    private static final class DocTooltip implements EditorDocTooltip {
        private LightweightHintImpl myHint;
        private final JComponent myComponent;
        private final @Nullable Consumer<? super String> myNewTextConsumer;
        private final Editor myEditor;
        private final int myOffset;

        DocTooltip(LightweightHintImpl hint, JComponent component, @Nullable Consumer<? super String> newTextConsumer, Editor editor, int offset) {
            myHint = hint;
            myComponent = component;
            myNewTextConsumer = newTextConsumer;
            myEditor = editor;
            myOffset = offset;
        }

        @RequiredUIAccess
        void show() {
            showHint(myHint);
        }

        @RequiredUIAccess
        private void showHint(LightweightHintImpl hint) {
            if (myEditor.isDisposed()) {
                return;
            }
            HintManagerEx hintManager = (HintManagerEx) HintManager.getInstance();
            short constraint = HintManager.ABOVE;
            LogicalPosition position = myEditor.offsetToLogicalPosition(myOffset);
            Point p = hintManager.getHintPosition(hint, myEditor, position, constraint);
            if (p.y - hint.getComponent().getPreferredSize().height < 0) {
                constraint = HintManager.UNDER;
                p = hintManager.getHintPosition(hint, myEditor, position, constraint);
            }
            hintManager.showEditorHint(hint, myEditor, p, HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0, false,
                hintManager.createHintHint(myEditor, p, hint, constraint).setContentActive(false));
        }

        @Override
        public boolean isVisible() {
            return myHint.isVisible();
        }

        @Override
        public void hide() {
            myHint.hide(true);
        }

        @Override
        public void addHideListener(Runnable runnable) {
            myHint.addHintListener(__ -> runnable.run());
        }

        @Override
        public void updateText(String html) {
            UIUtil.invokeLaterIfNeeded(() -> {
                if (myNewTextConsumer == null) {
                    return;
                }
                // There is a possible case that quick doc control width is changed, e.g. it contained text
                // like 'public final class String implements java.io.Serializable, java.lang.Comparable<java.lang.String>' and
                // new text replaces fully-qualified class names by hyperlinks with short name.
                // That's why we might need to update the control size. We assume that the hint component is located at the
                // layered pane, so, the algorithm is to find an ancestor layered pane and apply new size for the target component.
                Dimension oldSize = myComponent.getPreferredSize();
                myNewTextConsumer.accept(html);

                Dimension newSize = myComponent.getPreferredSize();
                if (newSize.width == oldSize.width) {
                    return;
                }
                myComponent.setPreferredSize(new Dimension(newSize.width, newSize.height));

                // We're assuming here that there are two possible hint representation modes: popup and layered pane.
                if (myHint.isRealPopup()) {
                    // There is a possible case that 'raw' control was rather wide but the 'rich' one is narrower. That's why we try to
                    // re-show the hint here. Benefits: there is a possible case that we'll be able to show nice layered pane-based balloon;
                    // the popup will be re-positioned according to the new width.
                    myHint.hide();
                    LightweightHintImpl newHint = new LightweightHintImpl(myComponent);
                    myHint = newHint;
                    showHint(newHint);
                    return;
                }

                Container topLevelLayeredPaneChild = null;
                boolean adjustBounds = false;
                for (Container current = myComponent.getParent(); current != null; current = current.getParent()) {
                    if (current instanceof JLayeredPane) {
                        adjustBounds = true;
                        break;
                    }
                    else {
                        topLevelLayeredPaneChild = current;
                    }
                }

                if (adjustBounds && topLevelLayeredPaneChild != null) {
                    Rectangle bounds = topLevelLayeredPaneChild.getBounds();
                    topLevelLayeredPaneChild.setBounds(bounds.x, bounds.y, bounds.width + newSize.width - oldSize.width, bounds.height);
                }
            });
        }

        @Override
        public boolean shouldSuppressMove(@Nullable Point2D prevScreenLocation, Point2D screenLocation) {
            Rectangle bounds = getHintBounds();
            if (bounds == null) {
                return false;
            }
            if (bounds.contains(screenLocation.x(), screenLocation.y())) {
                return true;
            }
            Point prevPoint = prevScreenLocation == null ? null : new Point(prevScreenLocation.x(), prevScreenLocation.y());
            return ScreenUtil.isMovementTowards(prevPoint, new Point(screenLocation.x(), screenLocation.y()), bounds);
        }

        private @Nullable Rectangle getHintBounds() {
            JComponent hintComponent = myHint.getComponent();
            if (!hintComponent.isShowing()) {
                return null;
            }
            return new Rectangle(hintComponent.getLocationOnScreen(), hintComponent.getSize());
        }
    }
}
