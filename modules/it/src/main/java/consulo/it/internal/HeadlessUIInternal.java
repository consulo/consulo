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
package consulo.it.internal;

import consulo.application.impl.internal.ModalityStateImpl;
import consulo.it.internal.ui.*;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.color.ColorValue;
import consulo.ui.font.FontManager;
import consulo.ui.image.EmptyImage;
import consulo.ui.image.Image;
import consulo.ui.image.ImageKey;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.impl.model.ImmutableListModelImpl;
import consulo.ui.impl.model.MutableListModelImpl;
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.*;
import consulo.ui.model.ListModel;
import consulo.ui.model.MutableListModel;
import consulo.ui.style.StyleManager;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Headless {@link UIInternal}: real UI-thread / modality resolution for integration tests, plus
 * dummy-but-creatable component/layout/widget factories so real {@code Unified*}/UI code can run
 * headlessly. The dummy components never render; they only hold trivial state.
 *
 * @author VISTALL
 */
public class HeadlessUIInternal extends UIInternal {
    @Override
    public CheckBox _Components_checkBox() {
        return new HeadlessCheckBox();
    }

    @Override
    public DockLayout _Layouts_dock(int gapInPixels) {
        return new HeadlessDockLayout();
    }

    @Override
    public WrappedLayout _Layouts_wrapped() {
        return new HeadlessWrappedLayout();
    }

    @Override
    public VerticalLayout _Layouts_vertical(int vGap) {
        return new HeadlessVerticalLayout();
    }

    @Override
    public SwipeLayout _Layouts_swipe() {
        return new HeadlessSwipeLayout();
    }

    @Override
    public TwoComponentSplitLayout _TwoComponentSplitLayout_create(SplitLayoutPosition position) {
        return new HeadlessTwoComponentSplitLayout();
    }

    @Override
    public ThreeComponentSplitLayout _ThreeComponentSplitLayout_create(SplitLayoutPosition position) {
        return new HeadlessThreeComponentSplitLayout();
    }

    @Override
    public TabbedLayout _Layouts_tabbed() {
        return new HeadlessTabbedLayout();
    }

    @Override
    public LabeledLayout _Layouts_labeled(LocalizeValue label) {
        return new HeadlessLabeledLayout(label);
    }

    @Override
    public HorizontalLayout _Layouts_horizontal(int gapInPixels) {
        return new HeadlessHorizontalLayout();
    }

    @Override
    public Label _Components_label(LocalizeValue text, LabelOptions options) {
        return new HeadlessLabel(text);
    }

    @Override
    public HtmlLabel _Components_htmlLabel(LocalizeValue html, LabelOptions labelOptions) {
        return new HeadlessHtmlLabel(html);
    }

    @Override
    public <E> ComboBox<E> _Components_comboBox(ListModel<E> model) {
        return new HeadlessComboBox<>(model);
    }

    @Override
    public TextBox _Components_textBox(String text) {
        return new HeadlessTextBox(text);
    }

    @Override
    public ProgressBar _Components_progressBar() {
        return new HeadlessProgressBar();
    }

    @Override
    public IntBox _Components_intBox(int value) {
        return new HeadlessIntBox(value);
    }

    @Override
    public <E> ListBox<E> _Components_listBox(ListModel<E> model) {
        return new HeadlessListBox<>(model);
    }

    @Override
    public ImageBox _Components_imageBox(Image image) {
        return new HeadlessImageBox(image);
    }

    @Override
    public ColorBox _Components_colorBox(@Nullable ColorValue colorValue) {
        return new HeadlessColorBox(colorValue);
    }

    @Override
    public Image _Image_lazy(Supplier<Image> imageSupplier) {
        return new HeadlessImage();
    }

    @Override
    public Image _ImageEffects_layered(Image[] images) {
        return new HeadlessImage();
    }

    @Override
    public Image _ImageEffects_transparent(Image original, float alpha) {
        return new HeadlessImage(original.getWidth(), original.getHeight());
    }

    @Override
    public Image _ImageEffects_grayed(Image original) {
        return new HeadlessImage(original.getWidth(), original.getHeight());
    }

    @Override
    public Image _ImageEffects_appendRight(Image i0, Image i1) {
        return new HeadlessImage(i0.getWidth() + i1.getWidth(), Math.max(i0.getHeight(), i1.getHeight()));
    }

    @Override
    public EmptyImage _ImageEffects_empty(int width, int height) {
        return new HeadlessEmptyImage(width, height);
    }

    @Override
    public Image _ImageEffects_canvas(int width, int height, Consumer<Canvas2D> consumer) {
        return new HeadlessImage(width, height);
    }

    @Override
    public Image _ImageEffects_withText(Image baseImage, String text) {
        return new HeadlessImage(baseImage.getWidth(), baseImage.getHeight());
    }

    @Override
    public Image _ImageEffects_resize(Image original, int width, int height) {
        return new HeadlessImage(width, height);
    }

    @Override
    public ImageKey _ImageKey_of(String groupId, String imageId, int width, int height) {
        return new HeadlessImageKey(groupId, imageId, width, height);
    }

    @Override
    public MenuSeparator _MenuSeparator_create() {
        return new HeadlessMenuSeparator();
    }

    @Override
    public ValueGroup<Boolean> _ValueGroups_boolGroup() {
        return new HeadlessValueGroup<>();
    }

    @Override
    public MenuBar _MenuItems_menuBar() {
        return new HeadlessMenuBar();
    }

    @Override
    public StyleManager _StyleManager_get() {
        return new HeadlessStyleManager();
    }

    @Override
    public FontManager _FontManager_get() {
        return new HeadlessFontManager();
    }

    @Override
    public Window _Window_create(String title, WindowOptions options) {
        return new HeadlessWindow(title);
    }

    @Override
    public @Nullable Window _Window_getActiveWindow() {
        return null;
    }

    @Override
    public <T> Alert<T> _Alerts_create() {
        return new HeadlessAlert<>();
    }

    @Override
    public <T> ListModel<T> _ListModel_create(Collection<? extends T> list) {
        return new ImmutableListModelImpl<>(list);
    }

    @Override
    public <T> MutableListModel<T> _MutableListModel_create(Collection<? extends T> list) {
        return new MutableListModelImpl<>(list);
    }

    @Override
    public TextBoxWithExpandAction _Components_textBoxWithExpandAction(Image editButtonImage,
                                                                      String dialogTitle,
                                                                      Function<String, List<String>> parser,
                                                                      Function<List<String>, String> joiner) {
        return new HeadlessTextBoxWithExpandAction();
    }

    @Override
    public TextBoxWithExtensions _Components_textBoxWithExtensions(String text) {
        return new HeadlessTextBoxWithExtensions(text);
    }

    @Override
    public FoldoutLayout _Layouts_foldout(LocalizeValue titleValue, Component component, boolean show) {
        return new HeadlessFoldoutLayout(titleValue, show);
    }

    @Override
    public UIAccess _UIAccess_get() {
        return HeadlessUIAccess.INSTANCE;
    }

    @Override
    public boolean _UIAccess_isUIThread() {
        return HeadlessUIAccess.INSTANCE.isUIThread();
    }

    @Override
    public ModalityState _ModalityState_any() {
        // mirror DesktopUIInternalImpl: return the constant directly — delegating to
        // IdeaModalityState.any() round-trips through Application.getAnyModalityState()
        // -> ModalityState.any() -> this method, i.e. infinite recursion
        return ModalityStateImpl.ANY;
    }

    @Override
    public ModalityState _ModalityState_nonModal() {
        return ModalityStateImpl.NON_MODAL;
    }
}
