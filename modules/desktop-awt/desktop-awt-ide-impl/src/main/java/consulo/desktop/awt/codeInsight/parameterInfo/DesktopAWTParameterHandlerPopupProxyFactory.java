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

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.impl.internal.parameterInfo.ParameterHandlerPopupProxy;
import consulo.language.editor.impl.internal.parameterInfo.ParameterHandlerPopupProxyFactory;
import consulo.language.editor.impl.internal.parameterInfo.ParameterInfoModel;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.hint.HintManager;
import consulo.project.Project;
import consulo.ui.RelativePoint2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.AsyncProcessIcon;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBLoadingPanel;
import consulo.ui.ex.awt.LoadingDecorator;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.AWT)
public class DesktopAWTParameterHandlerPopupProxyFactory implements ParameterHandlerPopupProxyFactory {
    @Override
    public ParameterHandlerPopupProxy create(Editor editor) {
        return new DesktopAWTParameterHandlerPopupProxy(editor);
    }

    @Override
    @RequiredUIAccess
    public void showLookupHint(Editor editor, ParameterInfoModel model, boolean requestFocus) {
        ParameterInfoComponent component = new ParameterInfoComponent(editor);
        component.setRequestFocus(requestFocus);
        component.setModel(model);

        LightweightHintImpl hint = new LightweightHintImpl(component);
        hint.setSelectingHint(true);

        Pair<Point, Short> pos = ParameterInfoHintPosition.chooseBestHintPosition(editor, null, hint, HintManager.DEFAULT, true);
        HintManagerImpl.getInstanceImpl().showEditorHint(
            hint,
            editor,
            pos.getFirst(),
            HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_LOOKUP_ITEM_CHANGE | HintManager.UPDATE_BY_SCROLLING,
            0,
            false,
            pos.getSecond()
        );
    }

    @Override
    @RequiredUIAccess
    public Runnable showLoading(Editor editor, String title, Runnable cancel) {
        Project project = editor.getProject();

        Disposable disposable = Disposable.newDisposable();
        if (project != null) {
            Disposer.register(project, disposable);
        }

        JBLoadingPanel loadingPanel = new JBLoadingPanel(
            null,
            panel -> new LoadingDecorator(panel, disposable, 0, false, new AsyncProcessIcon("ShowParameterInfo")) {
                @Override
                protected NonOpaquePanel customizeLoadingLayer(JPanel parent, JLabel text, AsyncProcessIcon icon) {
                    parent.setLayout(new FlowLayout(FlowLayout.LEFT));
                    NonOpaquePanel result = new NonOpaquePanel();
                    result.add(icon);
                    parent.add(result);
                    return result;
                }
            }
        );
        loadingPanel.add(new JBLabel(Image.empty(Image.DEFAULT_ICON_SIZE)));
        loadingPanel.add(new JBLabel(title));

        ComponentPopupBuilder builder = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(loadingPanel, null)
            .setProject(project)
            .setCancelCallback(() -> {
                cancel.run();
                return true;
            });
        JBPopup popup = builder.createPopup();
        Disposer.register(disposable, popup);

        RelativePoint2D popupPosition = EditorPopupHelper.getInstance().guessBestPopupLocation(editor);
        loadingPanel.startLoading();
        popup.show(popupPosition);

        return () -> {
            loadingPanel.stopLoading();
            if (popup.isVisible()) {
                popup.setUiVisible(false);
            }
            Disposer.dispose(disposable);
        };
    }
}
