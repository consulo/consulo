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
package consulo.desktop.qt.codeInsight.intention;

import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.codeEditor.internal.CaretPixelLocationProvider.CaretPixelLocation;
import consulo.language.editor.impl.internal.intention.IntentionListStep;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.internal.intention.IntentionManagerSettings;
import consulo.language.editor.refactoring.action.BaseRefactoringIntentionAction;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorImpl;
import consulo.desktop.qt.editor.impl.internal.DesktopQtEditorWidget;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.AnchoredPopup;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.awt.Rectangle;

/**
 * The bulb offering the intentions of the line the caret is on, and the list it opens.
 * <p>
 * Where it goes is asked of {@link CaretPixelLocationProvider}: the editor lays the text out itself but the popup
 * is placed against the ui component, and that is the one thing which answers in those coordinates.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtIntentionHintComponent implements Disposable {

    /**
     * How far into the text the bulb is set, so it does not sit against the gutter it follows.
     */
    private static final int GUTTER_GAP = 10;

    private final PsiFile myFile;
    private final Editor myEditor;
    private final CachedIntentions myCachedIntentions;

    private @Nullable DesktopQtIntentionBulbWidget myHint;
    private @Nullable CaretPixelLocation myHintLocation;

    private int myHintX;
    private int myHintY;
    private @Nullable Rectangle myHintVisibleArea;

    private @Nullable JBPopup myPopup;

    public DesktopQtIntentionHintComponent(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
        myFile = file;
        myEditor = editor;
        myCachedIntentions = cachedIntentions;
    }

    @RequiredUIAccess
    public void showHint() {
        CaretPixelLocation location = caretLocation();
        if (location == null) {
            return;
        }

        DesktopQtEditorWidget surface = myEditor instanceof DesktopQtEditorImpl qtEditor ? qtEditor.getSurface() : null;
        if (surface == null) {
            return;
        }

        // a child of the surface rather than a popup: a popup of the frontend brings a border, a background and a
        // layout which sizes it, none of which belongs on an icon sitting over a line of code
        DesktopQtIntentionBulbWidget hint = new DesktopQtIntentionBulbWidget(surface, bulbIcon(), this::showPopup);

        myHint = hint;
        myHintLocation = location;

        // the bulb belongs beside the line rather than beside the caret, which is what textX answers
        myHintX = location.textX() + GUTTER_GAP;

        myHintY = location.y();
        myHintVisibleArea = myEditor.getScrollingModel().getVisibleArea();

        myEditor.getScrollingModel().addVisibleAreaListener(this::editorScrolled, this);

        showBulbAt(hint, location);
    }

    /**
     * The bulb is placed against the component, and scrolling moves the line out from under it without the popup
     * being told - so it is moved by as much, and taken away once the line it belongs to has gone.
     */
    @RequiredUIAccess
    private void editorScrolled(VisibleAreaEvent event) {
        hidePopup();

        DesktopQtIntentionBulbWidget hint = myHint;
        CaretPixelLocation location = myHintLocation;
        Rectangle shownAt = myHintVisibleArea;

        if (hint == null || hint.isDisposed() || location == null || shownAt == null) {
            return;
        }

        Rectangle visibleArea = event.getNewRectangle();
        int shift = visibleArea.y - shownAt.y;

        myHintY -= shift;
        myHintLocation =
            new CaretPixelLocation(location.x(), location.y() - shift, location.height(), location.textX());
        myHintVisibleArea = visibleArea;

        if (myHintY + location.height() <= 0 || myHintY >= visibleArea.height) {
            hideHint();
            return;
        }

        showBulbAt(hint, location);
    }

    /**
     * The bulb is centred on the row it belongs to, and the coordinates it is given are those of the editor
     * component - the surface it hangs on begins at the same corner, so they carry straight over.
     */
    private void showBulbAt(DesktopQtIntentionBulbWidget hint, CaretPixelLocation location) {
        hint.applyGeometry(myHintX, Math.max(0, myHintY + (location.height() - hint.iconHeight()) / 2));

        hint.show();
        hint.raise();
    }

    @RequiredUIAccess
    public void showPopup() {
        CaretPixelLocation hintLocation = myHintLocation;

        hideHint();
        hidePopup();

        ListPopupStep step = new IntentionListStep(null, myEditor, myFile, myCachedIntentions.getProject(), myCachedIntentions);

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(myCachedIntentions.getProject(), step);
        myPopup = popup;

        // opened from the bulb, the list hangs under the line the bulb is on rather than at the caret, so it does
        // not cover what it was opened from
        if (hintLocation != null && popup instanceof AnchoredPopup anchoredPopup) {
            anchoredPopup.showAtPoint(
                myEditor.getUIComponent(),
                myHintX,
                hintLocation.y(),
                hintLocation.height()
            );
        }
        else {
            myEditor.showPopupInBestPositionFor(popup);
        }
    }

    @RequiredUIAccess
    @Override
    public void dispose() {
        hideHint();
        hidePopup();
    }

    @RequiredUIAccess
    private void hideHint() {
        DesktopQtIntentionBulbWidget hint = myHint;

        myHint = null;
        myHintLocation = null;
        myHintVisibleArea = null;

        if (hint != null && !hint.isDisposed()) {
            hint.hide();
            hint.disposeLater();
        }
    }

    @RequiredUIAccess
    private void hidePopup() {
        JBPopup popup = myPopup;
        myPopup = null;

        if (popup != null && !popup.isDisposed()) {
            popup.cancel();
        }
    }

    private @Nullable CaretPixelLocation caretLocation() {
        return myEditor instanceof CaretPixelLocationProvider provider ? provider.getCaretPixelLocation() : null;
    }

    private Image bulbIcon() {
        boolean refactoring = ContainerUtil.exists(
            myCachedIntentions.getInspectionFixes(),
            fix -> fix.getAction() instanceof BaseRefactoringIntentionAction
        );

        if (refactoring) {
            return PlatformIconGroup.actionsRefactoringbulb();
        }

        boolean quickFix = ContainerUtil.exists(
            myCachedIntentions.getErrorFixes(),
            fix -> IntentionManagerSettings.getInstance().isShowLightBulb(fix.getAction())
        );

        return quickFix ? PlatformIconGroup.actionsQuickfixbulb() : PlatformIconGroup.actionsIntentionbulb();
    }
}
