// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.editor;

import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayProperties;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.logging.Logger;
import consulo.review.editor.CodeReviewInlayModel;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Shows editor inlays constructed from view models using a component factory.
 * Does NOT guarantee the order.
 */
public final class CodeReviewEditorInlaysRenderer {
    private static final Logger LOG = Logger.getInstance(CodeReviewEditorInlaysRenderer.class);

    private CodeReviewEditorInlaysRenderer() {
    }

    @RequiredUIAccess
    public static <VM extends CodeReviewInlayModel> void renderInlays(Disposable parentDisposable,
                                                                      Editor editor,
                                                                      ObservableValue<? extends Collection<VM>> vms,
                                                                      BiFunction<Disposable, VM, @Nullable Component> rendererFactory) {
        Map<Object, Disposable> controllersByVmKey = new LinkedHashMap<>();
        Disposer.register(parentDisposable, () -> {
            for (Disposable controller : controllersByVmKey.values()) {
                Disposer.dispose(controller);
            }
            controllersByVmKey.clear();
        });

        ReviewUIUtil.bind(parentDisposable, vms, list -> {
            Set<Object> keys = new HashSet<>();
            for (VM vm : list) {
                keys.add(vm.getKey());
            }

            // remove missing
            Iterator<Map.Entry<Object, Disposable>> iterator = controllersByVmKey.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Object, Disposable> entry = iterator.next();
                if (!keys.contains(entry.getKey())) {
                    iterator.remove();
                    Disposer.dispose(entry.getValue());
                }
            }

            // add new
            for (VM vm : list) {
                if (controllersByVmKey.containsKey(vm.getKey())) {
                    continue;
                }
                Disposable controller = Disposable.newDisposable("Code review editor inlay for " + vm.getKey());
                controllersByVmKey.put(vm.getKey(), controller);
                controlInlay(controller, vm, editor, rendererFactory);
                CodeReviewEditorInlayRangeOutlineUtils.showOutlineIn(controller, editor, vm);
            }
        });
    }

    @RequiredUIAccess
    private static <VM extends CodeReviewInlayModel> void controlInlay(Disposable controller,
                                                                       VM vm,
                                                                       Editor editor,
                                                                       BiFunction<Disposable, VM, @Nullable Component> rendererFactory) {
        CodeReviewEditorInlayState state = new CodeReviewEditorInlayState();
        Disposer.register(controller, state::disposeRenderer);

        ReviewUIUtil.bind(controller, vm.getLine(), vm.isVisible(), (line, isVisible) -> {
            state.disposeInlay();
            if (line != null) {
                if (isVisible) {
                    Component component = state.myComponent;
                    if (component == null) {
                        Disposable rendererDisposable = Disposable.newDisposable("Code review editor inlay renderer for " + vm.getKey());
                        component = rendererFactory.apply(rendererDisposable, vm);
                        if (component == null) {
                            Disposer.dispose(rendererDisposable);
                            return;
                        }
                        state.myComponent = component;
                        state.myRendererDisposable = rendererDisposable;
                    }
                    state.myInlay = insertComponent(editor, component, line);
                }
            }
            else {
                state.disposeRenderer();
            }
        });
    }

    @RequiredUIAccess
    private static @Nullable Inlay<?> insertComponent(Editor editor, Component component, int line) {
        Document document = editor.getDocument();
        if (document.getLineCount() <= line) {
            LOG.warn("Failed to insert inlay into " + editor + " at line " + line);
            return null;
        }
        int offset = document.getLineEndOffset(line);
        InlayProperties properties = new InlayProperties().priority(0).relatesToPrecedingText(true);
        return editor.getInlayModel().addBlockElement(offset, properties, new CodeReviewComponentInlayRenderer(component));
    }
}
