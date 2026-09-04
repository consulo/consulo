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
package consulo.ui.ex.impl.internal;

import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.DialogCancelledException;
import consulo.ui.ImageBox;
import consulo.ui.InputProblem;
import consulo.ui.InputValidator;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.ValueComponent;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.BaseInputBoxBuilder;
import consulo.ui.impl.MessagePresentation;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * An input box assembled from the general components, for the frontends which have no input box of
 * their own and as the fallback for those whose own one cannot express a given request.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public class UnifiedInputBoxBuilderImpl<V, C extends ValueComponent<V>> extends BaseInputBoxBuilder<V, C> {
    private static final Space ICON_GAP = Space.X_LARGE;
    private static final Space BUTTON_GAP = Space.MEDIUM;

    private final Supplier<C> myEditorFactory;

    public UnifiedInputBoxBuilderImpl(Supplier<C> editorFactory) {
        myEditorFactory = editorFactory;
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        Window window = Window.create(MessagePresentation.title(myTitle).get(), WindowOptions.builder().owner(owner).build());

        C editor = myEditorFactory.get();
        if (myInitialValue != null) {
            editor.setValue(myInitialValue);
        }
        applySetup(editor);

        Label problemLabel = Label.create(LocalizeValue.empty());

        AtomicBoolean confirmed = new AtomicBoolean();

        Button confirmButton = Button.create(
            myConfirmText.isNotEmpty() ? myConfirmText : CommonLocalize.buttonOk(),
            event -> {
                confirmed.set(true);
                window.close();
            }
        );
        confirmButton.addStyle(ButtonStyle.PRIMARY);

        Button cancelButton = Button.create(
            myCancelText.isNotEmpty() ? myCancelText : CommonLocalize.buttonCancel(),
            event -> window.close()
        );

        // always, not only when a validator was given: a box with no value must not be confirmable,
        // or its answer would be null and indistinguishable from a dismissal
        editor.addValueListener(event -> revalidate(editor, problemLabel, confirmButton));
        revalidate(editor, problemLabel, confirmButton);

        window.setContent(buildContent(editor, problemLabel, confirmButton, cancelButton));

        CompletableFuture<V> result = new CompletableFuture<>();

        window.addCloseListener(event -> {
            V value = confirmed.get() ? normalize(editor.getValue()) : null;
            if (value != null) {
                result.complete(value);
            }
            else {
                result.completeExceptionally(new DialogCancelledException());
            }
        });

        window.show();

        return result;
    }

    @RequiredUIAccess
    private void revalidate(C editor, Label problemLabel, Button confirmButton) {
        V value = editor.getValue();
        if (value == null) {
            problemLabel.setText(LocalizeValue.empty());
            confirmButton.setEnabled(false);
            return;
        }

        InputValidator<V> validator = myValidator;
        InputProblem problem = validator == null ? null : validator.validate(value);

        problemLabel.setText(problem != null ? problem.message() : LocalizeValue.empty());
        confirmButton.setEnabled(problem == null || !problem.blocksConfirm());
    }

    @RequiredUIAccess
    private Component buildContent(C editor, Label problemLabel, Button confirmButton, Button cancelButton) {
        DockLayout root = DockLayout.create();

        HorizontalLayout body = HorizontalLayout.create(ICON_GAP);

        Image icon = myIcon != null ? myIcon : MessagePresentation.icon(mySeverity);
        if (icon != null) {
            body.add(ImageBox.create(icon));
        }

        VerticalLayout fields = VerticalLayout.create();
        if (myText.isNotEmpty()) {
            fields.add(Label.create(myText));
        }
        fields.add(editor);
        fields.add(problemLabel);
        body.add(fields);

        root.center(body);

        HorizontalLayout buttons = HorizontalLayout.create(BUTTON_GAP);
        buttons.add(confirmButton);
        buttons.add(cancelButton);

        DockLayout bottom = DockLayout.create();
        bottom.right(buttons);
        root.bottom(bottom);

        return root;
    }
}
