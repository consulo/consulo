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
package consulo.ide.impl.codeInsight.lookup;

import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.CaretPixelLocationProvider;
import consulo.codeEditor.internal.CaretPixelLocationProvider.CaretPixelLocation;
import consulo.language.editor.completion.lookup.LookupAdvertiser;
import consulo.language.editor.completion.lookup.LookupArranger;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.editor.impl.internal.completion.lookup.LookupBase;
import consulo.language.editor.impl.internal.completion.lookup.LookupItemRender;
import consulo.localize.LocalizeValue;
import consulo.application.ui.UISettings;
import consulo.project.Project;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.ListBox;
import consulo.ui.Label;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The lookup as the frontends without swing draw it - a {@link LightPopup} anchored to the caret, holding a
 * {@link ListBox} of the items. Everything about what the lookup means is in {@link LookupBase}; this only puts it
 * on screen.
 *
 * @author VISTALL
 */
public class UnifiedLookupUI extends LookupBase {

    // completion runs to hundreds of items, so the list is built to fetch around what it shows
    private final MutableFlatDataModel<LookupElement> myModel = FlatDataModel.lazyOf(List.of());
    private final ListBox<LookupElement> myList = ListBox.create(myModel);
    private final Label myAdvertisementLabel = Label.create(LocalizeValue.empty());
    private final UnifiedLookupAdvertiser myAdvertiser = new UnifiedLookupAdvertiser();

    private @Nullable LightPopup myPopup;

    @RequiredUIAccess
    public UnifiedLookupUI(Project project, Editor editor, LookupArranger arranger) {
        super(project, editor, arranger);

        // rendering only replays what was worked out off the ui thread - nothing here reaches for the psi
        myList.setRender(new LookupItemRender(this::getPresentation, this::itemPattern).asRender());

        // bounded before anything is pushed - the first refresh would otherwise hand the frontend every item there
        // is, and only then be told how many of them are shown
        myList.setVisibleRowCount(UISettings.getInstance().getMaxLookupListHeight());

        // the editor keeps the caret and the keys, so the list only follows - a click on a row is the one thing it
        // decides by itself
        myList.addValueListener(event -> {
            if (!isUpdating()) {
                markSelectionTouched();
            }
        });

        addEditorListeners();
    }

    // ------------------------------------------------------------------------------------------------
    // the list
    // ------------------------------------------------------------------------------------------------

    /**
     * The arranger is asked for the items again on every refresh, and answers with the same ones in the same order
     * whenever nothing it sorts by has changed - moving the selection with an arrow key is the common case. Handing
     * the model an equal copy still counts as a reset, and a reset is every row on screen built again, so a list
     * which did not change is left alone.
     */
    @Override
    protected void setItemsUi(List<LookupElement> items) {
        if (isSameAsModel(items)) {
            return;
        }

        myModel.replaceAll(items);
    }

    private boolean isSameAsModel(List<LookupElement> items) {
        if (items.size() != myModel.getSize()) {
            return false;
        }

        for (int i = 0; i < items.size(); i++) {
            // by identity, the way the base tells one refresh's items from another's
            if (myModel.get(i) != items.get(i)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected List<LookupElement> getItemsUi() {
        List<LookupElement> items = new ArrayList<>(myModel.getSize());
        for (LookupElement item : myModel) {
            items.add(item);
        }
        return items;
    }

    @Override
    protected int getItemsCountUi() {
        return myModel.getSize();
    }

    @Override
    protected int getSelectedIndexUi() {
        LookupElement value = myList.getValue();
        return value == null ? -1 : myModel.indexOf(value);
    }

    @Override
    protected void setSelectedIndexUi(int index) {
        if (index >= 0 && index < myModel.getSize()) {
            myList.setValueByIndex(index);
        }
    }

    @Override
    protected @Nullable LookupElement getSelectedValueUi() {
        return myList.getValue();
    }

    @Override
    protected void setSelectedValueUi(LookupElement item) {
        myList.setValue(item);
    }

    /**
     * The browser scrolls the list itself and does not report what is on screen, so the whole list counts as visible.
     * That makes {@link #ensureSelectionVisible} a no-op, which is right until the list is told to scroll.
     */
    @Override
    protected int getFirstVisibleIndexUi() {
        return myModel.getSize() == 0 ? -1 : 0;
    }

    @Override
    protected int getLastVisibleIndexUi() {
        return myModel.getSize() - 1;
    }

    @Override
    protected boolean isSelectionVisibleUi() {
        return true;
    }

    @Override
    protected void ensureIndexVisibleUi(int index) {
    }

    @Override
    protected void ensureRangeVisibleUi(int from, int to) {
    }

    /**
     * Nothing to cache here - the base holds the presentation, and the frontend measures no widths of its own.
     */
    @Override
    protected void itemAddedUi(LookupElement item, LookupElementPresentation presentation) {
    }

    @Override
    protected void presentationChangedUi(LookupElement item) {
        myModel.update(item);
    }

    /**
     * The whole batch in one go. Each {@code update} is a row the frontend has to be told about, and telling it is a
     * message - but every message raised inside one ui task travels in the same response, so a completion whose
     * hundreds of renders all land at once costs one round trip rather than one each.
     */
    @Override
    protected void presentationsChangedUi(List<LookupElement> items) {
        for (LookupElement item : items) {
            myModel.update(item);
        }
    }

    /**
     * How many rows the list shows at once - everything else is still there, a scroll away. Without it the list asks
     * for every item it holds and a few hundred completions fill the window.
     */
    @Override
    protected void updateListHeightUi() {
        myList.setVisibleRowCount(Math.min(myModel.getSize(), UISettings.getInstance().getMaxLookupListHeight()));
    }

    @Override
    protected void repaintUi() {
    }

    @Override
    protected void repaintLookupUi(boolean selectionVisible, boolean itemsChanged, boolean reused, boolean onExplicitAction) {
        myAdvertisementLabel.setText(LocalizeValue.of(myAdvertiser.currentText()));
    }

    // ------------------------------------------------------------------------------------------------
    // the popup
    // ------------------------------------------------------------------------------------------------

    @Override
    @RequiredUIAccess
    protected boolean showUi() {
        DockLayout content = DockLayout.create();
        content.center(ScrollableLayout.create(myList));
        content.bottom(myAdvertisementLabel);

        // the width a completion list is read at is not the width of what happens to be in it - a list which fits
        // itself to its longest item jumps about as the items change, so it opens at the width the user last left it
        content.setSize(new Size2D(UISettings.getInstance().getMaxLookupWidth(), -1));

        LightPopup popup = LightPopup.create(
            PopupOptions.builder()
                // the caret stays in the editor, which is what the lookup is driven from
                .disableRequestFocus()
                // escape is the popup's, the way it is on the desktop - the lookup is a hint there and the hint
                // manager takes the key for it. a handler of the lookup's own would have to be registered against
                // the editor action, which every frontend shares, and awt already answers escape for itself
                // and so does dismissing it - a lookup goes away when the caret moves or the document changes, which
                // it watches for itself. the popup never takes the focus, so every press reads as one from outside
                // it and picking a row would close the lookup before the row could be taken
                .disableCancelOnClickOutside()
                // a completion list is read at whatever width its items need, which only the user knows
                .resizable()
                .build()
        );
        popup.setContent(content);
        popup.addCloseListener(event -> {
            if (!isLookupDisposed()) {
                hideLookup(false);
            }
        });

        myPopup = popup;

        showAtCaret(popup);

        return true;
    }

    /**
     * Only the frontend knows where the caret ended up, so an editor which cannot say puts the popup at its top left
     * rather than refusing to open.
     */
    @RequiredUIAccess
    private void showAtCaret(LightPopup popup) {
        CaretPixelLocation location = myEditor instanceof CaretPixelLocationProvider provider
            ? provider.getCaretPixelLocation()
            : null;

        if (location == null) {
            popup.showAt(myEditor.getUIComponent(), 0, 0, 0);
        }
        else {
            popup.showAt(myEditor.getUIComponent(), location.x(), location.y(), location.height());
        }
    }

    @Override
    @RequiredUIAccess
    protected void hideUi() {
        LightPopup popup = myPopup;
        myPopup = null;
        if (popup != null && popup.isVisible()) {
            popup.close();
        }
    }

    @Override
    protected void repositionUi() {
        LightPopup popup = myPopup;
        if (popup != null && popup.isVisible()) {
            showAtCaret(popup);
        }
    }

    @Override
    public boolean isVisible() {
        LightPopup popup = myPopup;
        return popup != null && popup.isVisible();
    }

    @Override
    protected void setCalculatingUi(boolean calculating) {
    }

    @Override
    public void updateLookupWidth() {
    }

    @Override
    public boolean isPositionedAboveCaret() {
        // the browser decides which way round it fits and does not report back
        return false;
    }

    @Override
    public void setCancelOnClickOutside(boolean b) {
    }

    @Override
    public void setCancelOnOtherWindowOpen(boolean b) {
    }

    // ------------------------------------------------------------------------------------------------
    // moving through the list
    // ------------------------------------------------------------------------------------------------

    @Override
    public void moveUp() {
        moveBy(-1);
    }

    @Override
    public void moveDown() {
        moveBy(1);
    }

    @Override
    public void movePageUp() {
        moveBy(-PAGE_SIZE);
    }

    @Override
    public void movePageDown() {
        moveBy(PAGE_SIZE);
    }

    @Override
    public void moveHome() {
        setSelectedIndexUi(0);
    }

    @Override
    public void moveEnd() {
        setSelectedIndexUi(myModel.getSize() - 1);
    }

    /**
     * A page of rows, since the list does not say how many of them are on screen.
     */
    private static final int PAGE_SIZE = 10;

    private void moveBy(int delta) {
        int size = myModel.getSize();
        if (size == 0) {
            return;
        }

        int index = getSelectedIndexUi();
        if (index < 0) {
            setSelectedIndexUi(delta > 0 ? 0 : size - 1);
            return;
        }

        setSelectedIndexUi(Math.max(0, Math.min(size - 1, index + delta)));
    }

    // ------------------------------------------------------------------------------------------------
    // the advertisement strip
    // ------------------------------------------------------------------------------------------------

    @Override
    public LookupAdvertiser getAdvertiser() {
        return myAdvertiser;
    }

    @Override
    @RequiredUIAccess
    public void addAdvertisement(String text, @Nullable Image icon) {
        if (!containsDummyIdentifier(text)) {
            myAdvertiser.add(text);
            requestResize();
        }
    }

    @Override
    public List<String> getAdvertisements() {
        return myAdvertiser.texts();
    }

    private static class UnifiedLookupAdvertiser implements LookupAdvertiser {
        private final List<String> myTexts = new ArrayList<>();
        private int myCurrent = -1;

        @Override
        public void showRandomText() {
            myCurrent = myTexts.isEmpty() ? -1 : ThreadLocalRandom.current().nextInt(myTexts.size());
        }

        @Override
        public void clearAdvertisements() {
            myTexts.clear();
            myCurrent = -1;
        }

        void add(String text) {
            myTexts.add(text);
            if (myCurrent < 0) {
                myCurrent = 0;
            }
        }

        String currentText() {
            return myCurrent < 0 || myCurrent >= myTexts.size() ? "" : myTexts.get(myCurrent);
        }

        List<String> texts() {
            return Collections.unmodifiableList(myTexts);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // what only swing can answer
    // ------------------------------------------------------------------------------------------------

    /**
     * There is no awt component behind this lookup. The callers which ask - the debugger's inplace editor and the
     * dominant hint area of the text editor - are both awt only and read it as "nothing there", which is right.
     */
    @Override
    public @Nullable Component getComponent() {
        return null;
    }

    @Override
    public @Nullable Rectangle getBounds() {
        return null;
    }

    @Override
    public @Nullable Rectangle getCurrentItemBounds() {
        return null;
    }

    @Override
    public void showElementActions(@Nullable InputEvent event) {
    }
}
