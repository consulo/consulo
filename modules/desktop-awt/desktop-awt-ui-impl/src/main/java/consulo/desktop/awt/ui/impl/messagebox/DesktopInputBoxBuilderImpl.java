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
package consulo.desktop.awt.ui.impl.messagebox;

import consulo.localize.LocalizeValue;
import consulo.ui.DialogCancelledException;
import consulo.ui.InputProblem;
import consulo.ui.InputValidator;
import consulo.ui.ValueComponent;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.impl.BaseInputBoxBuilder;
import consulo.ui.impl.MessagePresentation;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * An input box drawn as a {@link DialogWrapper} around the general editor component, so the editor
 * a caller configures is the one it actually gets.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public class DesktopInputBoxBuilderImpl<V, C extends ValueComponent<V>> extends BaseInputBoxBuilder<V, C> {
    private final Supplier<C> myEditorFactory;

    public DesktopInputBoxBuilderImpl(Supplier<C> editorFactory) {
        myEditorFactory = editorFactory;
    }

    private class DialogImpl extends DialogWrapper {
        private final C myEditor;

        DialogImpl(java.awt.@Nullable Component parent, C editor) {
            super(parent != null ? parent : null, false);
            myEditor = editor;
            setTitle(MessagePresentation.title(myTitle).get());

            if (myConfirmText.isNotEmpty()) {
                setOKButtonText(myConfirmText);
            }
            if (myCancelText.isNotEmpty()) {
                setCancelButtonText(myCancelText);
            }

            init();
            revalidate();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(15, 0));

            Image icon = myIcon != null ? myIcon : MessagePresentation.icon(mySeverity);
            if (icon != null) {
                Container container = new Container();
                container.setLayout(new BorderLayout());
                container.add(new JBLabel(icon), BorderLayout.NORTH);
                panel.add(container, BorderLayout.WEST);
            }

            JPanel fields = new JPanel(new BorderLayout(0, 6));
            if (myText.isNotEmpty()) {
                fields.add(new JBLabel(myText.get()), BorderLayout.NORTH);
            }
            fields.add(TargetAWT.to(myEditor), BorderLayout.CENTER);
            panel.add(fields, BorderLayout.CENTER);

            return panel;
        }

        @Override
        @RequiredUIAccess
        protected void doOKAction() {
            InputValidator<V> validator = myValidator;
            V value = myEditor.getValue();

            if (validator == null || value == null) {
                super.doOKAction();
                return;
            }

            setOKActionEnabled(false);

            validator.confirm(value).whenComplete((problem, error) -> {
                setOKActionEnabled(true);

                if (error != null) {
                    setErrorText(LocalizeValue.of(String.valueOf(error.getMessage())));
                    return;
                }

                if (problem != null && problem.blocksConfirm()) {
                    setErrorText(problem.message());
                    return;
                }

                super.doOKAction();
            });
        }

        @RequiredUIAccess
        void revalidate() {
            V value = myEditor.getValue();

            // a box with no value must not be confirmable, or its answer would be null and
            // indistinguishable from a dismissal
            if (value == null) {
                setErrorText(LocalizeValue.empty());
                setOKActionEnabled(false);
                return;
            }

            InputValidator<V> validator = myValidator;
            InputProblem problem = validator == null ? null : validator.validate(value);

            setErrorText(problem != null ? problem.message() : LocalizeValue.empty());
            setOKActionEnabled(problem == null || !problem.blocksConfirm());
        }

        C editor() {
            return myEditor;
        }
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        C editor = myEditorFactory.get();
        if (myInitialValue != null) {
            editor.setValue(myInitialValue);
        }
        applySetup(editor);

        DialogImpl dialog = new DialogImpl(owner != null ? TargetAWT.to(owner) : null, editor);

        // the initial state has to reflect the validator, which a change listener alone never does
        editor.addValueListener(event -> dialog.revalidate());

        CompletableFuture<V> result = new CompletableFuture<>();

        dialog.showAsync().whenComplete((ignored, error) -> {
            if (error != null) {
                result.completeExceptionally(error);
                return;
            }

            V value = dialog.isOK() ? normalize(dialog.editor().getValue()) : null;
            if (value != null) {
                result.complete(value);
            }
            else {
                result.completeExceptionally(new DialogCancelledException());
            }
        });

        return result;
    }
}
