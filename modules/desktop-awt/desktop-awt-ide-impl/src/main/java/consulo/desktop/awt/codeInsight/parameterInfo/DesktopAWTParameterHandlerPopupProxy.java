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
package consulo.desktop.awt.codeInsight.parameterInfo;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.language.editor.internal.parameterInfo.ParameterHandlerPopupProxy;
import consulo.language.editor.internal.parameterInfo.ParameterInfoAnchor;
import consulo.language.editor.internal.parameterInfo.ParameterInfoModel;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.inject.EditorWindow;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.internal.IdeTooltip;
import consulo.ui.ex.popup.Balloon.Position;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class DesktopAWTParameterHandlerPopupProxy implements ParameterHandlerPopupProxy {
    private final Editor myEditor;
    private final ParameterInfoComponent myComponent;
    private final ParameterInfoHintPosition myPosition;
    private LightweightHintImpl myHint;

    public DesktopAWTParameterHandlerPopupProxy(Editor editor) {
        myEditor = editor;
        myComponent = new ParameterInfoComponent(editor);
        myPosition = new ParameterInfoHintPosition(editor);
        myHint = createHint();
    }

    private LightweightHintImpl createHint() {
        JPanel wrapper = new ParameterInfoWrapperPanel();
        wrapper.add(myComponent);

        LightweightHintImpl hint = new LightweightHintImpl(wrapper);
        hint.setSelectingHint(true);
        return hint;
    }

    @Override
    public boolean isVisible() {
        return myHint.isVisible();
    }

    @Override
    @RequiredUIAccess
    public void show(ParameterInfoModel model, @Nullable ParameterInfoAnchor anchor, boolean requestFocus, boolean hideByTextChange) {
        if (myHint.isVisible()) {
            myHint.getComponent().removeAll();
            myHint.hide();
            myHint = createHint();
        }

        myComponent.setRequestFocus(requestFocus);
        myComponent.setModel(model);

        int caretOffset = myEditor.getCaretModel().getOffset();
        Pair<Point, Short> pos = myPosition.getBestPointPosition(myHint, anchor, caretOffset, null, HintManager.ABOVE);

        HintHint hintHint = HintManagerImpl.getInstanceImpl().createHintHint(myEditor, pos.getFirst(), myHint, pos.getSecond());
        hintHint.setExplicitClose(true);
        hintHint.setRequestFocus(requestFocus);
        hintHint.setShowImmediately(true);
        hintHint.setBorderColor(ParameterInfoComponent.BORDER_COLOR);
        hintHint.setBorderInsets(JBUI.insets(4, 1, 4, 1));
        hintHint.setComponentBorder(JBUI.Borders.empty());

        int flags = HintManager.HIDE_BY_ESCAPE | HintManager.UPDATE_BY_SCROLLING;
        if (hideByTextChange) {
            flags |= HintManager.HIDE_BY_TEXT_CHANGE;
        }

        Editor editorToShow = myEditor instanceof EditorWindow editorWindow ? editorWindow.getDelegate() : myEditor;

        HintManagerImpl.getInstanceImpl().showEditorHint(myHint, editorToShow, pos.getFirst(), flags, 0, false, hintHint);
    }

    @Override
    @RequiredUIAccess
    public void update(ParameterInfoModel model, @Nullable ParameterInfoAnchor anchor) {
        myComponent.setModel(model);

        if (myEditor.getComponent().getRootPane() == null && !Application.get().isUnitTestMode()) {
            return;
        }

        IdeTooltip tooltip = myHint.getCurrentIdeTooltip();
        short position = tooltip != null ? toShort(tooltip.getPreferredPosition()) : HintManager.ABOVE;
        Pair<Point, Short> pos = myPosition.getBestPointPosition(
            myHint,
            anchor,
            myEditor.getCaretModel().getOffset(),
            myEditor.getCaretModel().getVisualPosition(),
            position
        );
        HintManagerImpl.getInstanceImpl().adjustEditorHintPosition(myHint, myEditor, pos.getFirst(), pos.getSecond());
    }

    @Override
    @RequiredUIAccess
    public void adjustForLookup(Lookup lookup) {
        IdeTooltip tooltip = myHint.getCurrentIdeTooltip();
        if (tooltip == null) {
            return;
        }

        JRootPane root = myEditor.getComponent().getRootPane();
        if (root == null) {
            return;
        }

        Point p = tooltip.getShowingPoint().getPoint(root.getLayeredPane());
        if (lookup.isPositionedAboveCaret()) {
            if (Position.above == tooltip.getPreferredPosition()) {
                myHint.pack();
                myHint.updatePosition(Position.below);
                myHint.updateLocation(p.x, p.y + tooltip.getPositionChangeY());
            }
        }
        else {
            if (Position.below == tooltip.getPreferredPosition()) {
                myHint.pack();
                myHint.updatePosition(Position.above);
                myHint.updateLocation(p.x, p.y - tooltip.getPositionChangeY());
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void hide() {
        myHint.hide();
    }

    @HintManager.PositionFlags
    private static short toShort(Position position) {
        return switch (position) {
            case above -> HintManager.ABOVE;
            case atLeft -> HintManager.LEFT;
            case atRight -> HintManager.RIGHT;
            default -> HintManager.UNDER;
        };
    }
}
