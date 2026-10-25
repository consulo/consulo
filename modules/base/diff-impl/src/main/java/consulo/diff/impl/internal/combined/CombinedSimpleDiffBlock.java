// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedPathBlockId;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Card;
import consulo.ui.CardStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.WrappedLayout;
import consulo.ui.style.ComponentColors;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CombinedSimpleDiffBlock implements CombinedCollapsibleDiffBlock<CombinedPathBlockId> {
    private final CombinedPathBlockId myId;

    private final List<CombinedDiffBlockListener> myCollapsingListeners = new CopyOnWriteArrayList<>();

    private final CombinedSimpleDiffHeader myPathOnlyHeader;
    private final CombinedSimpleDiffHeader myHeaderWithToolbar;
    private final CombinedSimpleDiffHeader myStickyHeader;
    private final Card myStickyHeaderComponent;
    private final Button myStickyCollapseButton;

    private final WrappedLayout myHeader;
    private final WrappedLayout myBody;
    private final Card myComponent;
    private final Button myCollapseButton;

    private Component myContent;

    private boolean myBlockCollapsed;
    private boolean myBlockSelected;

    @RequiredUIAccess
    public CombinedSimpleDiffBlock(Project project, CombinedPathBlockId id, Component content, boolean isPathOnlyHeader, boolean isCollapsed) {
        myId = id;
        myContent = content;
        myBlockCollapsed = isCollapsed;

        myPathOnlyHeader = new CombinedSimpleDiffHeader(project, id, true, this::toggleCollapse);
        myHeaderWithToolbar = new CombinedSimpleDiffHeader(project, id, false, this::toggleCollapse);
        myStickyHeader = new CombinedSimpleDiffHeader(project, id, false, this::toggleCollapse);

        myHeader = WrappedLayout.create(isPathOnlyHeader ? myPathOnlyHeader.getComponent() : myHeaderWithToolbar.getComponent());
        myBody = WrappedLayout.create();
        if (!isCollapsed) {
            myBody.set(content);
        }

        myCollapseButton = Button.create(LocalizeValue.empty());
        myCollapseButton.addStyle(ButtonStyle.INPLACE);
        myCollapseButton.addClickListener(event -> toggleCollapse());

        myStickyCollapseButton = Button.create(LocalizeValue.empty());
        myStickyCollapseButton.addStyle(ButtonStyle.INPLACE);
        myStickyCollapseButton.addClickListener(event -> toggleCollapse());
        updateCollapseIcon();

        myStickyHeaderComponent = Card.create();
        myStickyHeaderComponent.addStyle(CardStyle.OUTLINED);
        myStickyHeaderComponent.addStyle(CardStyle.COMPACT);
        myStickyHeaderComponent.setBackgroundColor(ComponentColors.LAYOUT);
        myStickyHeaderComponent.setHeaderPrefix(myStickyCollapseButton);
        myStickyHeaderComponent.setHeader(myStickyHeader.getComponent());
        myStickyHeaderComponent.setHeaderSuffix(myStickyHeader.getToolbarComponent());

        myComponent = Card.create();
        myComponent.addStyle(CardStyle.OUTLINED);
        myComponent.addStyle(CardStyle.COMPACT);
        myComponent.setHeaderPrefix(myCollapseButton);
        myComponent.setHeader(myHeader);
        myComponent.setHeaderSuffix(isPathOnlyHeader ? null : myHeaderWithToolbar.getToolbarComponent());
        myComponent.add(myBody);

        setHeaderToolbarTargetComponent(content);
    }

    @Override
    public CombinedPathBlockId getId() {
        return myId;
    }

    @Override
    public Component getHeader() {
        return myHeader;
    }

    @Override
    public Component getStickyHeaderComponent() {
        return myStickyHeaderComponent;
    }

    @Override
    public Component getPreferredFocusComponent() {
        return myHeader;
    }

    @Override
    public Component getBody() {
        return myBody;
    }

    @Override
    public Component getComponent() {
        return myComponent;
    }

    @Override
    public boolean isCollapsed() {
        return myBlockCollapsed;
    }

    @RequiredUIAccess
    @Override
    public void setSelected(boolean selected) {
        if (myBlockSelected == selected) {
            return;
        }
        myBlockSelected = selected;
        if (selected) {
            myComponent.addStyle(CardStyle.SELECTED);
            myStickyHeaderComponent.addStyle(CardStyle.SELECTED);
        }
        else {
            myComponent.removeStyle(CardStyle.SELECTED);
            myStickyHeaderComponent.removeStyle(CardStyle.SELECTED);
        }
    }

    public boolean isSelected() {
        return myBlockSelected;
    }

    @RequiredUIAccess
    @Override
    public void setCollapsed(boolean collapsed) {
        if (myBlockCollapsed == collapsed) {
            return;
        }
        myBlockCollapsed = collapsed;
        updateBodyContent(collapsed);
        updateCollapseIcon();
        for (CombinedDiffBlockListener listener : myCollapsingListeners) {
            listener.onCollapseStateChanged(myId, collapsed);
        }
    }

    @Override
    public void addListener(CombinedDiffBlockListener listener, Disposable parentDisposable) {
        myCollapsingListeners.add(listener);
        Disposer.register(parentDisposable, () -> myCollapsingListeners.remove(listener));
    }

    @RequiredUIAccess
    private void setHeaderToolbarTargetComponent(Component component) {
        myHeaderWithToolbar.setToolbarTargetComponent(component);
        myStickyHeader.setToolbarTargetComponent(component);
    }

    @RequiredUIAccess
    @Override
    public void updateBlockContent(CombinedDiffBlockContent newContent) {
        myContent = newContent.getViewer().getComponent();
        setHeaderToolbarTargetComponent(myContent);
        if (!myBlockCollapsed) {
            myBody.set(myContent);
        }
        boolean loading = newContent.getViewer() instanceof CombinedDiffLoadingBlock;
        myHeader.set(loading ? myPathOnlyHeader.getComponent() : myHeaderWithToolbar.getComponent());
        myComponent.setHeaderSuffix(loading ? null : myHeaderWithToolbar.getToolbarComponent());
    }

    @RequiredUIAccess
    private void toggleCollapse() {
        setCollapsed(!myBlockCollapsed);
    }

    @RequiredUIAccess
    private void updateBodyContent(boolean newCollapsingState) {
        if (newCollapsingState) {
            myBody.set((Component) null);
        }
        else {
            myBody.set(myContent);
        }
    }

    @RequiredUIAccess
    private void updateCollapseIcon() {
        Image icon = myBlockCollapsed ? PlatformIconGroup.generalArrowright() : PlatformIconGroup.generalArrowdown();
        myCollapseButton.setIcon(icon);
        myStickyCollapseButton.setIcon(icon);
    }

    @Override
    public void dispose() {
    }
}
