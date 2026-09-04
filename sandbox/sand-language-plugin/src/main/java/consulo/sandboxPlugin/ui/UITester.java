/*
 * Copyright 2013-2020 consulo.io
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
package consulo.sandboxPlugin.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.cursor.StandardCursors;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.layout.*;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.style.StandardColors;
import consulo.util.lang.ThreeState;
import consulo.util.lang.TimeoutUtil;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.*;
import consulo.ui.MessageBoxes;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputValidators;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageBoxRemember;
import consulo.ui.MessageButtonRole;
import consulo.ui.UIAccess;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import consulo.ui.DialogCancelledException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * @author VISTALL
 * @since 2020-05-29
 */
public class UITester {
    private static final SandRemember ourRemember = new SandRemember();

    private static class SandRemember implements MessageBoxRemember<Boolean> {
        private @Nullable Boolean myValue;

        @Override
        public void setValue(@Nullable Boolean value) {
            myValue = value;
        }

        @Override
        public @Nullable Boolean getValue() {
            return myValue;
        }

        @Override
        public LocalizeValue getMessageText() {
            return LocalizeValue.of("Do not ask again");
        }
    }

    private static class MyWindowWrapper extends DialogDescriptor {
        public MyWindowWrapper() {
            super(LocalizeValue.of("UI Tester"));
        }

        @Override
        @RequiredUIAccess
        public Component createCenterComponent(Disposable uiDisposable) {
            TabbedLayout tabbedLayout = TabbedLayout.create();

            tabbedLayout.addTab("Layouts", layouts(uiDisposable)).setCloseHandler((tab, component) -> {
            });
            tabbedLayout.addTab("Components", components());
            tabbedLayout.addTab("Components > Table", table());
            tabbedLayout.addTab("Components > Tree", tree(uiDisposable));
            tabbedLayout.addTab("Message Boxes", alerts());
            tabbedLayout.addTab("Inputs", inputs());
            tabbedLayout.addTab("DelayedAction", delayedAction());

            return tabbedLayout;
        }

        @RequiredUIAccess
        private Component delayedAction() {
            VerticalLayout layout = VerticalLayout.create();
            layout.add(Label.create(LocalizeValue.localizeTODO("The indicator is drawn where the click happened, for two seconds")));
            layout.add(Button.create(LocalizeValue.localizeTODO("Start"), e -> {
                DelayedAction action = DelayedAction.start(e);
                UIAccess.current().getScheduler().schedule(action::stop, 2, TimeUnit.SECONDS);
            }));
            return layout;
        }

        @RequiredUIAccess
        private Component layouts(Disposable uiDisposable) {
            TabbedLayout tabbedLayout = TabbedLayout.create();

            VerticalLayout fold = VerticalLayout.create();
            fold.add(Label.create("Some label"));
            fold.add(Button.create(LocalizeValue.localizeTODO("Some &Button"), e -> MessageBoxes.okError(LocalizeValue.of("Clicked!")).showAsync()));

            FoldoutLayout layout = FoldoutLayout.create(LocalizeValue.of("Show Me"), fold);
            layout.addOpenedListener(it -> MessageBoxes.okInfo(LocalizeValue.of("State " + it.isOpened())).showAsync());

            tabbedLayout.addTab("FoldoutLayout", layout);

            TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
            splitLayout.setFirstComponent(DockLayout.create().center(Button.create("Left")));
            splitLayout.setSecondComponent(DockLayout.create().center(Button.create("Second")));

            tabbedLayout.addTab("SplitLayout", splitLayout);

            SwipeLayout swipeLayout = SwipeLayout.create();

            swipeLayout.register("left", () -> swipeChildLayout(LocalizeValue.of("Right"), () -> swipeLayout.swipeRightTo("right")));
            swipeLayout.register("right", () -> swipeChildLayout(LocalizeValue.of("Left"), () -> swipeLayout.swipeLeftTo("left")));

            tabbedLayout.addTab("SwipeLayout", swipeLayout);

            VerticalLayout borderLayout = VerticalLayout.create();
            DockLayout dockLayout = DockLayout.create();
            Button centerBtn = Button.create(LocalizeValue.of("Center"));
            centerBtn.addClickListener(
                event -> dockLayout.center(HorizontalLayout.create().add(Label.create(LocalizeValue.of(LocalDateTime.now().toString()))))
            );

            borderLayout.add(centerBtn).add(dockLayout);

            tabbedLayout.addTab("DockLayout", borderLayout);
            tabbedLayout.addTab("LoadingLayout", loadingLayout(uiDisposable));

            return tabbedLayout;
        }

        @Override
        public boolean hasDefaultContentBorder() {
            return false;
        }

        @RequiredUIAccess
        private Layout swipeChildLayout(LocalizeValue text, @RequiredUIAccess Runnable runnable) {
            DockLayout dockLayout = DockLayout.create();

            dockLayout.center(HorizontalLayout.create().add(Button.create(text, e -> runnable.run())));

            return dockLayout;
        }

        @RequiredUIAccess
        private Component components() {
            VerticalLayout layout = VerticalLayout.create();

            FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(null);
            layout.add(builder.build());

            ToggleSwitch toggleSwitch = ToggleSwitch.create(true);
            toggleSwitch.addValueListener(event -> MessageBoxes.okInfo(LocalizeValue.of("toggle")).showAsync());

            CheckBox checkBox = CheckBox.create(LocalizeValue.of("Check box"));
            checkBox.addValueListener(event -> MessageBoxes.okInfo(LocalizeValue.of("checkBox")).showAsync());
            checkBox.setToolTipText(LocalizeValue.of("Some Tooltip"));

            layout.add(AdvancedLabel.create().updatePresentation(presentation -> {
                presentation.append(LocalizeValue.of("Advanced "), TextAttribute.REGULAR_BOLD);
                presentation.append(
                    LocalizeValue.of("Label"),
                    new TextAttribute(Font.PLAIN, StandardColors.RED, StandardColors.BLACK)
                );
            }));

            layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("Toggle Switch"))).add(toggleSwitch).add(checkBox));

            TriStateCheckBox triStateCheckBox = TriStateCheckBox.create(LocalizeValue.of("Tri state"));
            triStateCheckBox.addValueListener(
                event -> MessageBoxes.okInfo(LocalizeValue.of("triStateCheckBox " + event.getValue())).showAsync()
            );

            TriStateCheckBox twoStateCheckBox = TriStateCheckBox.create(LocalizeValue.of("Unsure disabled"), ThreeState.UNSURE);
            twoStateCheckBox.setUnsureEnabled(false);
            twoStateCheckBox.addValueListener(
                event -> MessageBoxes.okInfo(LocalizeValue.of("twoStateCheckBox " + event.getValue())).showAsync()
            );

            layout.add(HorizontalLayout.create()
                .add(Label.create(LocalizeValue.of("TriStateCheckBox")))
                .add(triStateCheckBox)
                .add(twoStateCheckBox));

            layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("Password"))).add(PasswordBox.create()));

            ProgressBar spinnerBar = ProgressBar.create();
            spinnerBar.addStyle(ProgressBarStyle.SPINNER);
            spinnerBar.setIndeterminate(true);

            layout.add(HorizontalLayout.create()
                .add(Label.create(LocalizeValue.of("Spinner Progress")))
                .add(spinnerBar));

            IntSlider intSlider = IntSlider.create(3);
            intSlider.addValueListener(event -> MessageBoxes.okInfo(LocalizeValue.of("intSlider " + event.getValue())).showAsync());
            layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("IntSlider"))).add(intSlider));

            DatePicker datePicker = DatePicker.create();
            datePicker.setValue(new Date());
            datePicker.addValueListener(event -> MessageBoxes.okInfo(LocalizeValue.of("datePicker " + event.getValue())).showAsync());
            layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("DatePicker"))).add(datePicker));

            layout.add(HtmlLabel.create(LocalizeValue.of("<b>Html</b> <i>Label</i>")));

            TextBoxWithExtensions textBoxWithExtensions = TextBoxWithExtensions.create("with extensions");
            textBoxWithExtensions.addLastExtension(new TextBoxWithExtensions.Extension(
                false,
                PlatformIconGroup.actionsFind(),
                null,
                event -> MessageBoxes.okInfo(LocalizeValue.of("extension clicked")).showAsync()
            ));
            layout.add(HorizontalLayout.create()
                .add(Label.create(LocalizeValue.of("TextBox With Extensions")))
                .add(textBoxWithExtensions));

            TextBoxWithExpandAction textBoxWithExpandAction = TextBoxWithExpandAction.create(
                null,
                "Edit Lines",
                text -> List.of(text.split(";")),
                lines -> String.join(";", lines)
            );
            textBoxWithExpandAction.setValue("one;two;three");
            layout.add(HorizontalLayout.create()
                .add(Label.create(LocalizeValue.of("TextBox With Expand")))
                .add(textBoxWithExpandAction));

            layout.add(Hyperlink.create(
                LocalizeValue.localizeTODO("Some Link"),
                (e) -> MessageBoxes.okInfo(LocalizeValue.of("Clicked!!!")).showAsync()
            ));

            HtmlView component = HtmlView.create();
            component.render(new HtmlView.RenderData("<html><body><b>Some Bold Text</b> Test</body></html>"));
            layout.add(component);
            return layout;
        }

        @RequiredUIAccess
        private Component table() {
            DockLayout layout = DockLayout.create();

            // the rows are the keys, so the cell values live outside the model and an edit is a write into these maps
            Map<String, String> values = new TreeMap<>();
            values.put("test1", "1");
            values.put("test2", "3");
            values.put("test3", "5");

            Map<String, ThreeState> states = new TreeMap<>();
            states.put("test1", ThreeState.YES);
            states.put("test2", ThreeState.UNSURE);
            states.put("test3", ThreeState.NO);

            MutableFlatDataModel<String> model = FlatDataModel.of(new ArrayList<>(values.keySet()));

            Table<String> table = Table.create(model);

            // a component column which is also editable - a check box carries its whole value in the click, so it has
            // to commit there and then rather than wait for the edit to be left
            table.addColumn(LocalizeValue.localizeTODO("On"), key -> states.getOrDefault(key, ThreeState.NO))
                .setWidth(40)
                .setResizable(false)
                .setRender(ComponentItemRender.reusable(
                    () -> TriStateCheckBox.create(LocalizeValue.empty()),
                    (checkBox, item) -> checkBox.setValue(item.getValue() == null ? ThreeState.NO : item.getValue())))
                .setEditor(new TableItemEditor<>() {
                    @Override
                    @RequiredUIAccess
                    public ValueComponent<ThreeState> createComponent(String key) {
                        TriStateCheckBox checkBox =
                            TriStateCheckBox.create(LocalizeValue.empty(), states.getOrDefault(key, ThreeState.NO));
                        checkBox.setUnsureEnabled(true);
                        return checkBox;
                    }

                    @Override
                    @RequiredUIAccess
                    public void commit(String key, @Nullable ThreeState value) {
                        states.put(key, value == null ? ThreeState.NO : value);
                        model.update(key);
                    }
                });

            table.addColumn(LocalizeValue.localizeTODO("Key"), key -> key)
                .setSortable(Comparator.naturalOrder())
                .setWidth(160);

            // a text column which is also editable - the opposite case, where the value is only settled once typing stops
            table.addColumn(LocalizeValue.localizeTODO("Value"), values::get)
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setSortable(Comparator.naturalOrder())
                .setRender((presentation, item) -> presentation.append(
                    String.valueOf(item.getValue()),
                    item.isSelected() ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR
                ))
                .setEditor(new TableItemEditor<>() {
                    @Override
                    @RequiredUIAccess
                    public ValueComponent<String> createComponent(String key) {
                        return TextBox.create(values.get(key));
                    }

                    @Override
                    @RequiredUIAccess
                    public void commit(String key, @Nullable String value) {
                        values.put(key, value == null ? "" : value);
                        model.update(key);
                    }
                });

            table.setSelectionMode(SelectionMode.MULTIPLE);
            table.setSpeedSearchConverter(key -> key);
            table.addSelectListener(event -> MessageBoxes.okInfo(LocalizeValue.of("Selected: " + event.getValues().size())).showAsync());

            layout.center(ScrollableLayout.create(table));

            return layout;
        }

        @RequiredUIAccess
        private Component loadingLayout(Disposable uiDisposable) {
            DockLayout layout = DockLayout.create();

            DockLayout innerLayout = DockLayout.create();

            LoadingLayout<DockLayout> loadingLayout = LoadingLayout.create(innerLayout, uiDisposable);

            Button start = Button.create(LocalizeValue.of("Start"), event -> loadingLayout.startLoading());

            Button stop = Button.create(
                LocalizeValue.of("Stop"),
                event -> loadingLayout.stopLoading(dockLayout -> {
                    dockLayout.removeAll();

                    dockLayout.center(Label.create(LocalizeValue.of(LocalDateTime.now().toString())));
                })
            );

            Button startPooled = Button.create(
                LocalizeValue.of("Start Pooled"),
                event -> loadingLayout.startLoading(
                    () -> {
                        TimeoutUtil.sleep(10000);
                        return "Some Value after 10 seconds";
                    },
                    (dockLayout, someValue) -> dockLayout.center(Label.create(LocalizeValue.of(someValue)))
                )
            );

            layout.top(HorizontalLayout.create().add(start).add(stop).add(startPooled));
            layout.center(loadingLayout);
            return layout;
        }

        @RequiredUIAccess
        private Component tree(Disposable uiDisposable) {
            Tree<String> tree = Tree.create(
                (TreeModel<String>) (nodeFactory, parentValue) -> {
                    if (parentValue == null) {
                        for (int i = 0; i < 50; i++) {
                            TreeNode<String> node = nodeFactory.apply("First Child = " + i);

                            List<Image> icons = List.of(
                                PlatformIconGroup.nodesClass(),
                                PlatformIconGroup.nodesEnum(),
                                PlatformIconGroup.nodesStruct(),
                                PlatformIconGroup.nodesInterface(),
                                PlatformIconGroup.nodesAttribute()
                            );
                            int r = new Random().nextInt(icons.size());

                            node.setRenderer((s, textItemPresentation) -> {
                                textItemPresentation.append(s);
                                textItemPresentation.withIcon(icons.get(r));
                            });
                        }
                    }
                    else {
                        for (int i = 0; i < 10; i++) {
                            nodeFactory.apply(parentValue + ", second child = " + i);
                        }
                    }
                }
            );
            Disposer.register(uiDisposable, tree.destroyHook());

            return ScrollableLayout.create(tree);
        }

        @RequiredUIAccess
        private Component alerts() {
            VerticalLayout layout = VerticalLayout.create();
            layout.add(
                Button.create(LocalizeValue.of("Info. Hand Cursor"), event -> MessageBoxes.okInfo(LocalizeValue.of("This is INFO")).showAsync())
                    .withCursor(StandardCursors.HAND)
            );
            layout.add(Button.create(LocalizeValue.of("Warning"), event -> MessageBoxes.okWarning(LocalizeValue.of("This is WARN")).showAsync()));
            layout.add(
                Button.create(
                        LocalizeValue.of("Error. Wait Cursor"),
                        event -> MessageBoxes.okError(LocalizeValue.of("This is ERROR")).showAsync()
                    )
                    .withCursor(StandardCursors.WAIT)
            );
            layout.add(Button.create(
                LocalizeValue.of("Question"),
                event -> MessageBoxes.okQuestion(LocalizeValue.of("This is QUESTION")).showAsync()
            ));

            layout.add(Button.create(
                LocalizeValue.of("Yes / No"),
                event -> MessageBoxes.yesNo()
                    .text(LocalizeValue.of("Proceed?"))
                    .showAsync()
                    .whenComplete((answer, error) -> report("yesNo", answer, error))
            ));

            layout.add(Button.create(
                LocalizeValue.of("Yes / No / Cancel"),
                event -> MessageBoxes.yesNoCancel()
                    .text(LocalizeValue.of("Save before closing?"))
                    .showAsync()
                    .whenComplete((answer, error) -> report("yesNoCancel", answer, error))
            ));

            // a standard role wearing its own label keeps its placement
            layout.add(Button.create(
                LocalizeValue.of("Custom labels on standard roles"),
                event -> {
                    MessageBoxBuilder<String> box = MessageBoxBuilder.create();
                    box.asQuestion();
                    box.text(LocalizeValue.of("A file of that name already exists."));
                    box.button(MessageButtonRole.YES, LocalizeValue.of("Overwrite"), "overwrite");
                    box.asDefaultButton();
                    box.button(MessageButtonRole.NO, LocalizeValue.of("Skip"), "skip");
                    box.button(MessageButtonRole.YES_TO_ALL, LocalizeValue.of("Overwrite All"), "overwriteAll");
                    box.button(MessageButtonRole.NO_TO_ALL, LocalizeValue.of("Skip All"), "skipAll");
                    box.button(MessageButtonRole.CANCEL, "cancel");
                    box.asExitButton();
                    box.showAsync().whenComplete((answer, error) -> report("customLabels", answer, error));
                }
            ));

            layout.add(Button.create(
                LocalizeValue.of("Detail (collapsible)"),
                event -> MessageBoxes.okError(LocalizeValue.of("The operation failed."))
                    .detail(LocalizeValue.of("java.lang.IllegalStateException: nothing here\n\tat sand.Tester.run(Tester.java:1)"))
                    .showAsync()
            ));

            layout.add(Button.create(
                LocalizeValue.of("Remember my choice"),
                event -> {
                    MessageBoxBuilder<Boolean> box = MessageBoxBuilder.create();
                    box.asQuestion();
                    box.text(LocalizeValue.of("Remembering this answer skips the box next time."));
                    box.button(MessageButtonRole.YES, Boolean.TRUE);
                    box.asDefaultButton();
                    box.button(MessageButtonRole.NO, Boolean.FALSE);
                    box.asExitButton();
                    box.remember(ourRemember);
                    box.showAsync().whenComplete((answer, error) -> report("remember", answer, error));
                }
            ));

            layout.add(Button.create(
                LocalizeValue.of("Forget remembered answer"),
                event -> ourRemember.setValue(null)
            ));

            layout.add(Button.create(
                LocalizeValue.of("Rich text"),
                event -> MessageBoxes.okInfo(LocalizeValue.of("<html><b>Bold</b> and <i>italic</i>.</html>"))
                    .richText()
                    .showAsync()
            ));

            layout.add(Button.create(
                LocalizeValue.of("Dismissed after 3s"),
                event -> {
                    CompletableFuture<?> shown =
                        MessageBoxes.okInfo(LocalizeValue.of("This closes itself in three seconds.")).showAsync();

                    UIAccess.current().getScheduler().schedule(() -> shown.cancel(false), 3, TimeUnit.SECONDS);
                }
            ));

            return layout;
        }

        private Component inputs() {
            VerticalLayout layout = VerticalLayout.create();

            layout.add(Button.create(
                LocalizeValue.of("Text"),
                event -> InputBoxBuilder.text()
                    .title(LocalizeValue.of("Rename"))
                    .text(LocalizeValue.of("New name:"))
                    .value("current")
                    .showAsync()
                    .whenComplete((answer, error) -> report("text", answer, error))
            ));

            layout.add(Button.create(
                LocalizeValue.of("Text, validated non-empty"),
                event -> InputBoxBuilder.text()
                    .text(LocalizeValue.of("Name (required):"))
                    .validator(InputValidators.nonEmpty(LocalizeValue.of("A name is required")))
                    .showAsync()
                    .whenComplete((answer, error) -> report("validated", answer, error))
            ));

            layout.add(Button.create(
                LocalizeValue.of("Integer, ranged 1..64"),
                event -> InputBoxBuilder.integer()
                    .text(LocalizeValue.of("How many threads?"))
                    .value(4)
                    .setupComponent(box -> box.withRange(1, 64))
                    .showAsync()
                    .whenComplete((answer, error) -> report("integer", answer, error))
            ));

            layout.add(Button.create(
                LocalizeValue.of("Password"),
                event -> InputBoxBuilder.password()
                    .text(LocalizeValue.of("Master password:"))
                    .showAsync()
                    .whenComplete((answer, error) -> report("password", answer == null ? null : "*".repeat(answer.length()), error))
            ));

            layout.add(Button.create(
                LocalizeValue.of("One of a list"),
                event -> InputBoxBuilder.items(List.of(StandardCursors.values()))
                    .text(LocalizeValue.of("Pick a cursor:"))
                    .setupComponent(box -> box.setRender((presentation, item) -> presentation.append(item.getValue().name())))
                    .showAsync()
                    .whenComplete((answer, error) -> report("items", answer, error))
            ));

            return layout;
        }

        @RequiredUIAccess
        private static void report(String what, @Nullable Object answer, @Nullable Throwable error) {
            Throwable cause = error instanceof CompletionException || error instanceof ExecutionException
                ? error.getCause()
                : error;

            if (cause instanceof DialogCancelledException) {
                MessageBoxes.okInfo(LocalizeValue.of(what + " -> cancelled")).showAsync();
            }
            else if (cause != null) {
                MessageBoxes.okError(LocalizeValue.of(what + " failed: " + cause.getMessage())).showAsync();
            }
            else {
                MessageBoxes.okInfo(LocalizeValue.of(what + " -> " + answer)).showAsync();
            }
        }

        @Override
        public @Nullable WidthAndHeight getInitialSize() {
            return WidthAndHeight.ofFont(25, 25);
        }
    }

    @RequiredUIAccess
    public static void show(DialogService dialogService) {
        dialogService.build(new MyWindowWrapper()).showAsync();
    }
}
