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
package consulo.web.internal.codeInsight.intention;

import com.vaadin.flow.dom.Element;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.codeEditor.internal.CaretPixelLocationProvider.CaretPixelLocation;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.codeInsight.intention.impl.IntentionListStep;
import consulo.language.editor.internal.intention.CachedIntentions;
import consulo.language.editor.internal.intention.IntentionManagerSettings;
import consulo.language.editor.refactoring.action.BaseRefactoringIntentionAction;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ImageBox;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.internal.AnchoredPopup;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.ListPopupStep;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.util.collection.ContainerUtil;
import consulo.web.internal.ui.base.TargetVaadin;

import java.awt.Rectangle;
import org.jspecify.annotations.Nullable;

/**
 * The intentions of one editor - the bulb which offers them and the list they are chosen from. The counterpart of
 * {@code IntentionHintComponent} of the awt frontend, which holds the same pair.
 *
 * @author VISTALL
 * @since 2026-08-09
 */
public class WebIntentionHintComponent implements Disposable {
    /**
     * How far into the text the bulb is set, so it does not sit against the gutter it follows.
     */
    private static final int GUTTER_GAP = 10;

    /**
     * What a vaadin popover leaves between itself and the thing it hangs from, measured - the bulb is meant to sit
     * on the line rather than under it, so the anchor is raised by as much.
     */
    private static final int POPOVER_GAP = 4;

    /**
     * What {@code --consulo-hint-padding} keeps around the icon, which stands between the top of the popup and the
     * top of the icon in it.
     */
    private static final int HINT_PADDING = 3;

    private final PsiFile myFile;
    private final Editor myEditor;
    private final CachedIntentions myCachedIntentions;

    private @Nullable LightPopup myHint;
    private @Nullable CaretPixelLocation myHintLocation;

    private int myHintX;
    private int myHintY;
    private @Nullable Rectangle myHintVisibleArea;

    private @Nullable JBPopup myPopup;

    public WebIntentionHintComponent(PsiFile file, Editor editor, CachedIntentions cachedIntentions) {
        myFile = file;
        myEditor = editor;
        myCachedIntentions = cachedIntentions;
    }

    /**
     * Only the bulb is shown until the pointer is over it, and the arrow which says a list is behind it appears
     * then - the awt hint keeps the arrow hidden the same way.
     */
    @RequiredUIAccess
    public void showHint() {
        CaretPixelLocation location = caretLocation();
        if (location == null) {
            return;
        }

        // the bulb only offers - taking the focus for it would put the editor behind what the user is typing in
        LightPopup hint = LightPopup.create(PopupOptions.builder().disableRequestFocus().build());

        // nothing of the popup itself is wanted around the bulb until the pointer is over it, only the bulb
        Element hintElement = TargetVaadin.to(hint).getElement();
        hintElement.getClassList().add("consulo-transparent-popup");

        ImageBox bulb = ImageBox.create(bulbIcon());

        ImageBox arrow = ImageBox.create(PlatformIconGroup.generalArrowdown());
        arrow.setVisible(false);

        HorizontalLayout content = HorizontalLayout.create(0);
        content.add(bulb);
        content.add(arrow);

        content.addClickListener(event -> showPopup());

        Element element = TargetVaadin.to(content).getElement();
        element.addEventListener("mouseenter", event -> {
            arrow.setVisible(true);
            hintElement.getClassList().add("consulo-hovered-popup");
        });
        element.addEventListener("mouseleave", event -> {
            arrow.setVisible(false);
            hintElement.getClassList().remove("consulo-hovered-popup");
        });

        hint.setContent(content);

        myHint = hint;
        myHintLocation = location;

        // a popup hangs under what it is anchored to, so anchoring at the line the caret is on would put the bulb
        // on the line after it - the anchor ends where the icon starts, which is the line of the caret, and the
        // icon is set down by what the line has over it so it reads as being on the line rather than above it
        int iconTop = location.y() + Math.max(0, (location.height() - bulbIcon().getHeight()) / 2);

        myHintX = location.textX() + GUTTER_GAP;
        myHintY = Math.max(0, iconTop - HINT_PADDING - POPOVER_GAP - location.height());
        myHintVisibleArea = myEditor.getScrollingModel().getVisibleArea();

        myEditor.getScrollingModel().addVisibleAreaListener(this::editorScrolled, this);

        hint.showAt(myEditor.getUIComponent(), myHintX, myHintY, location.height());
    }

    @RequiredUIAccess
    private void editorScrolled(VisibleAreaEvent event) {
        hidePopup();

        LightPopup hint = myHint;
        CaretPixelLocation location = myHintLocation;
        Rectangle shownAt = myHintVisibleArea;

        if (hint == null || location == null || shownAt == null || !hint.isVisible()) {
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

        hint.showAt(myEditor.getUIComponent(), myHintX, myHintY, location.height());
    }

    @RequiredUIAccess
    public void showPopup() {
        CaretPixelLocation hintLocation = myHintLocation;

        // the bulb stays while its list is open - it is what the list was opened from, and taking it away as the
        // list appears reads as the click having dismissed it
        hidePopup();

        ListPopupStep step = new IntentionListStep(null, myEditor, myFile, myCachedIntentions.getProject(), myCachedIntentions);

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(myCachedIntentions.getProject(), step);
        myPopup = popup;

        // opened from the bulb, the list belongs under the bulb rather than at the caret - which is where the awt
        // hint puts it as well, from the corner of its panel
        if (hintLocation != null && popup instanceof AnchoredPopup anchoredPopup) {
            // the whole line the bulb is on is what the list hangs under, so it does not cover the bulb it was
            // opened from
            anchoredPopup.showAtPoint(
                myEditor.getUIComponent(),
                myHintX,
                hintLocation.y() + hintLocation.height(),
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
        LightPopup hint = myHint;
        myHint = null;
        myHintLocation = null;

        // a popup which was asked to show is not visible until the browser answers, and one closed only when it
        // says it is visible is left open behind the next one
        if (hint != null) {
            hint.close();
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
