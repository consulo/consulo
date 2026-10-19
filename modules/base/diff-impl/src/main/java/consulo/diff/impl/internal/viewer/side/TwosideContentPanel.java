/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.viewer.side;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.bridge.EditorBridge;
import consulo.codeEditor.internal.bridge.EditorBridgeFactory;
import consulo.diff.impl.internal.viewer.EditorHolder;
import consulo.diff.impl.internal.viewer.TextEditorHolder;
import consulo.diff.util.Side;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.WrappedLayout;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TwosideContentPanel {
    private final Component myComponent;
    private final @Nullable EditorBridge myBridge;
    private final List<WrappedLayout> myTitles = new ArrayList<>();

    @RequiredUIAccess
    private TwosideContentPanel(EditorBridge bridge) {
        myBridge = bridge;
        myComponent = bridge.getComponent();
    }

    @RequiredUIAccess
    private TwosideContentPanel(List<? extends EditorHolder> holders) {
        myBridge = null;

        TwoComponentSplitLayout splitter = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        for (Side side : Side.values()) {
            WrappedLayout title = WrappedLayout.create();
            myTitles.add(title);

            DockLayout panel = DockLayout.create(Space.NONE);
            panel.top(title);
            panel.center(side.select(holders).getComponent());
            if (side == Side.LEFT) {
                splitter.setFirstComponent(panel);
            }
            else {
                splitter.setSecondComponent(panel);
            }
        }
        myComponent = splitter;
    }

    public Component getComponent() {
        return myComponent;
    }

    public @Nullable EditorBridge getBridge() {
        return myBridge;
    }

    @RequiredUIAccess
    public void setTitles(List<? extends @Nullable Component> titleComponents) {
        if (myBridge != null) {
            myBridge.setTitles(titleComponents);
            return;
        }
        for (Side side : Side.values()) {
            side.select(myTitles).set(side.select(titleComponents));
        }
    }

    @RequiredUIAccess
    public static TwosideContentPanel createFromHolders(List<? extends EditorHolder> holders, Disposable parentDisposable) {
        if (ContainerUtil.and(holders, holder -> holder instanceof TextEditorHolder)) {
            List<Editor> editors = ContainerUtil.map(holders, holder -> ((TextEditorHolder) holder).getEditor());
            EditorBridgeFactory factory = Application.get().getInstance(EditorBridgeFactory.class);
            return new TwosideContentPanel(factory.create(editors, parentDisposable));
        }
        return new TwosideContentPanel(holders);
    }
}
