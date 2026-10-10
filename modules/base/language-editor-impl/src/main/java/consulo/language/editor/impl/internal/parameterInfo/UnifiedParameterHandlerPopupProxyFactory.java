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
package consulo.language.editor.impl.internal.parameterInfo;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.completion.lookup.event.LookupListener;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedParameterHandlerPopupProxyFactory implements ParameterHandlerPopupProxyFactory {
    @Override
    public ParameterHandlerPopupProxy create(Editor editor) {
        return new UnifiedParameterHandlerPopupProxy(editor);
    }

    @Override
    @RequiredUIAccess
    public void showLookupHint(Editor editor, ParameterInfoModel model, boolean requestFocus) {
        LightPopup popup = UnifiedParameterInfoContent.createPopup(model);

        Disposable listeners = Disposable.newDisposable();
        popup.addCloseListener(event -> Disposer.dispose(listeners));

        Runnable close = () -> {
            if (!Disposer.isDisposed(listeners)) {
                Disposer.dispose(listeners);
                popup.close();
            }
        };

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent event) {
                close.run();
            }
        }, listeners);

        CaretListener caretListener = new CaretListener() {
            @Override
            public void caretPositionChanged(CaretEvent event) {
                close.run();
            }
        };
        editor.getCaretModel().addCaretListener(caretListener);
        Disposer.register(listeners, () -> editor.getCaretModel().removeCaretListener(caretListener));

        Project project = editor.getProject();
        Lookup lookup = project == null ? null : LookupManager.getInstance(project).getActiveLookup();
        if (lookup != null) {
            LookupListener lookupListener = new LookupListener() {
                @Override
                public void currentItemChanged(LookupEvent event) {
                    close.run();
                }

                @Override
                public void itemSelected(LookupEvent event) {
                    close.run();
                }

                @Override
                public void lookupCanceled(LookupEvent event) {
                    close.run();
                }
            };
            lookup.addLookupListener(lookupListener);
            Disposer.register(listeners, () -> lookup.removeLookupListener(lookupListener));
        }

        new UnifiedCaretAnchor(editor, popup).relocate();
    }

    @Override
    @RequiredUIAccess
    public Runnable showLoading(Editor editor, String title, Runnable cancel) {
        LightPopup popup = LightPopup.create(PopupOptions.builder().disableRequestFocus().build());

        Label label = Label.create(LocalizeValue.of(title));
        label.setImage(Image.busy());
        popup.setContent(label);

        boolean[] hiding = {false};
        popup.addCloseListener(event -> {
            if (!hiding[0]) {
                cancel.run();
            }
        });

        new UnifiedCaretAnchor(editor, popup).relocate();

        return () -> {
            if (!hiding[0]) {
                hiding[0] = true;
                popup.close();
            }
        };
    }
}
