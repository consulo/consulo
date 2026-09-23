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
package consulo.ide.impl.wm.impl;

import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowSettings;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.ThreeComponentSplitLayout;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The pair of splitters a tool window panel is built from, and which of the two is the outer one. The tool
 * windows of the outer splitter reach the whole side of the frame, and the ones of the inner splitter only
 * the part the outer one leaves them - so widescreen layout, which gives the whole height to the left and
 * right windows, is the horizontal splitter taken outside, and the ordinary layout is the vertical one.
 * <p/>
 * The editor lives in the centre of the inner splitter either way, which is why it is taken out and put back
 * when the two trade places.
 *
 * @author VISTALL
 */
public final class UnifiedToolWindowSplitters {
    private static final Logger LOG = Logger.getInstance(UnifiedToolWindowSplitters.class);

    private final Project myProject;
    private final Consumer<ThreeComponentSplitLayout> myRootConsumer;

    private final ThreeComponentSplitLayout myHorizontalSplitter =
        ThreeComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);

    private final ThreeComponentSplitLayout myVerticalSplitter =
        ThreeComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);

    private final Map<ToolWindowAnchor, Component> myAnchorComponents = new LinkedHashMap<>();

    private boolean myWidescreen;

    private @Nullable Component myDocumentComponent;

    /**
     * @param rootConsumer told which splitter is the outer one whenever that changes, so the panel holding it
     *                     can put the new one where the old one was
     */
    @RequiredUIAccess
    public UnifiedToolWindowSplitters(Project project, Disposable parentDisposable, Consumer<ThreeComponentSplitLayout> rootConsumer) {
        myProject = project;
        myRootConsumer = rootConsumer;
        myWidescreen = ToolWindowSettings.getInstance(project).isWidescreenSupport();

        nest();

        // the toolkit bound panel is told of the change by a dispatcher which walks the swing tree looking for
        // listeners; a frontend drawing its own components has no such tree to walk, so it subscribes itself
        Application.get().getMessageBus().connect(parentDisposable).subscribe(
            UISettingsListener.class,
            (UISettingsListener)this::uiSettingsChanged
        );
    }

    public ThreeComponentSplitLayout getHorizontalSplitter() {
        return myHorizontalSplitter;
    }

    public ThreeComponentSplitLayout getVerticalSplitter() {
        return myVerticalSplitter;
    }

    public ThreeComponentSplitLayout getRootSplitter() {
        return myWidescreen ? myHorizontalSplitter : myVerticalSplitter;
    }

    /**
     * Where a tool window of the given anchor belongs - left and right are the sides of the horizontal
     * splitter, top and bottom the sides of the vertical one, whichever of the two is the outer one.
     */
    @RequiredUIAccess
    public void setComponent(ToolWindowAnchor anchor, @Nullable Component component) {
        if (component == null) {
            myAnchorComponents.remove(anchor);
        }
        else {
            myAnchorComponents.put(anchor, component);
        }

        applyComponent(anchor, component);
    }

    @RequiredUIAccess
    private void applyComponent(ToolWindowAnchor anchor, @Nullable Component component) {
        if (ToolWindowAnchor.TOP == anchor) {
            myVerticalSplitter.setFirstComponent(component);
        }
        else if (ToolWindowAnchor.BOTTOM == anchor) {
            myVerticalSplitter.setSecondComponent(component);
        }
        else if (ToolWindowAnchor.LEFT == anchor) {
            myHorizontalSplitter.setFirstComponent(component);
        }
        else if (ToolWindowAnchor.RIGHT == anchor) {
            myHorizontalSplitter.setSecondComponent(component);
        }
        else {
            LOG.error("unknown anchor: " + anchor);
        }
    }

    @RequiredUIAccess
    public void setDocumentComponent(@Nullable Component component) {
        myDocumentComponent = component;
        innerSplitter().setCenterComponent(component);
    }

    @RequiredUIAccess
    private void uiSettingsChanged(UISettings uiSettings) {
        if (myWidescreen == ToolWindowSettings.getInstance(myProject).isWidescreenSupport()) {
            return;
        }

        Component documentComponent = myDocumentComponent;

        // everything the two splitters hold comes out before they trade places, and goes back in after - a
        // side left in place keeps the proportion it was given while it was the other way round
        for (ToolWindowAnchor anchor : myAnchorComponents.keySet()) {
            applyComponent(anchor, null);
        }
        innerSplitter().setCenterComponent(null);
        outerSplitter().setCenterComponent(null);

        myWidescreen = !myWidescreen;

        nest();
        setDocumentComponent(documentComponent);
        myAnchorComponents.forEach(this::applyComponent);

        myRootConsumer.accept(getRootSplitter());
    }

    @RequiredUIAccess
    private void nest() {
        outerSplitter().setCenterComponent(innerSplitter());
    }

    private ThreeComponentSplitLayout outerSplitter() {
        return myWidescreen ? myHorizontalSplitter : myVerticalSplitter;
    }

    private ThreeComponentSplitLayout innerSplitter() {
        return myWidescreen ? myVerticalSplitter : myHorizontalSplitter;
    }
}
