// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.avatar.Avatar;
import com.intellij.collaboration.ui.codereview.avatar.CodeReviewAvatarUtils;
import consulo.application.util.DateFormatUtil;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.IdeTooltipManager;
import icons.CollaborationToolsIcons;
import icons.DvcsImplIcons;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApiStatus.Internal
final class ReviewListCellRenderer<T> extends SelectablePanel implements ListCellRenderer<T> {
    private static final int MAX_PARTICIPANT_ICONS = 2;

    private final Function<T, ReviewListItemPresentation> presenter;
    private final ReviewListCellUiOptions options;

    private final JLabel titleSpacer = new JLabel();
    private final JLabel unseen = new JLabel();
    private final JLabel title = new JLabel();
    private final JLabel info = new JLabel();
    private final JLabel tags = new JLabel();
    private final SingleValueModel<@Nls String> stateTextModel = new SingleValueModel<>(null);
    private final JComponent stateLabel = CollaborationToolsUIUtil.createTagLabel(stateTextModel);
    private final JLabel nonMergeable = new JLabel();
    private final JLabel buildStatus = new JLabel();
    private final JLabel userGroup1 = new JLabel();
    private final JLabel userGroup2 = new JLabel();
    private final JLabel comments = new JLabel();

    private boolean isNewUI = false;

    ReviewListCellRenderer(@Nonnull Function<T, ReviewListItemPresentation> presenter) {
        this(presenter, new ReviewListCellUiOptions());
    }

    ReviewListCellRenderer(
        @Nonnull Function<T, ReviewListItemPresentation> presenter,
        @Nonnull ReviewListCellUiOptions options
    ) {
        super(null);
        this.presenter = presenter;
        this.options = options;

        titleSpacer.setPreferredSize(new JBDimension(1, CodeReviewAvatarUtils.expectedIconHeight(Avatar.Sizes.OUTLINED)));
        unseen.setIcon(new UnreadDotIcon());
        unseen.setBorder(new JBEmptyBorder(0, 2, 0, 0));
        title.setMinimumSize(new JBDimension(30, 0));
        info.setFont(FontUtil.minusOne(JBFont.create(info.getFont(), false)));

        JPanel firstLinePanel = new JPanel(new HorizontalSidesLayout(6));
        firstLinePanel.setOpaque(false);
        firstLinePanel.add(unseen, SwingConstants.LEFT);
        firstLinePanel.add(title, SwingConstants.LEFT);
        firstLinePanel.add(tags, SwingConstants.LEFT);

        firstLinePanel.add(titleSpacer, SwingConstants.RIGHT);
        firstLinePanel.add(stateLabel, SwingConstants.RIGHT);
        firstLinePanel.add(nonMergeable, SwingConstants.RIGHT);
        firstLinePanel.add(buildStatus, SwingConstants.RIGHT);
        firstLinePanel.add(userGroup1, SwingConstants.RIGHT);
        firstLinePanel.add(userGroup2, SwingConstants.RIGHT);
        firstLinePanel.add(comments, SwingConstants.RIGHT);

        setLayout(new BorderLayout());
        add(firstLinePanel, BorderLayout.CENTER);
        add(info, BorderLayout.SOUTH);

        UIUtil.forEachComponentInHierarchy(this, comp -> comp.setFocusable(false));
    }

    private @Nonnull IdeTooltipManager getToolTipManager() {
        return IdeTooltipManager.getInstance();
    }

    private void updateRendering() {
        int hSelectionInsets = !options.isBordered() ? 0 : 13;
        int hBorder;
        if (!options.isBordered()) {
            hBorder = 6;
        }
        else if (isNewUI) {
            hBorder = 19;
        }
        else {
            hBorder = 13;
        }

        if (isNewUI) {
            setBorder(JBUI.Borders.empty(4, hBorder, 5, hBorder));
            setSelectionArc(JBUI.CurrentTheme.Popup.Selection.ARC.get());
            setSelectionArcCorners(SelectionArcCorners.ALL);
            setSelectionInsets(JBInsets.create(0, hSelectionInsets));
        }
        else {
            setBorder(JBUI.Borders.empty(4, hBorder, 5, hBorder));
            setSelectionArc(0);
            setSelectionArcCorners(SelectionArcCorners.ALL);
            setSelectionInsets(JBInsets.create(0, 0));
        }
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends T> list,
        T value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        boolean newUIVal = ExperimentalUI.isNewUI();
        if (isNewUI != newUIVal) {
            isNewUI = newUIVal;
        }
        setBackground(list.getBackground());
        setSelectionColor(ListUiUtil.WithTallRow.background(list, isSelected, list.hasFocus()));
        Color primaryTextColor = ListUiUtil.WithTallRow.foreground(isSelected, list.hasFocus());
        Color secondaryTextColor = ListUiUtil.WithTallRow.secondaryForeground(isSelected && !ExperimentalUI.isNewUI(), list.hasFocus());

        if (value == null) {
            return this;
        }
        ReviewListItemPresentation presentation = presenter.apply(value);
        if (presentation == null) {
            return this;
        }

        Boolean seenVal = presentation.getSeen();
        unseen.setVisible(seenVal != null && !seenVal);

        title.setText(presentation.getTitle());
        title.setForeground(primaryTextColor);
        addTruncationListener(title);

        UserPresentation author = presentation.getAuthor();
        if (author != null) {
            info.setText(CollaborationToolsLocalize.reviewListInfoAuthor(
                presentation.getId(),
                DateFormatUtil.formatPrettyDate(presentation.getCreatedDate()),
                author.getPresentableName()
            ).get());
        }
        else {
            info.setText(CollaborationToolsLocalize.reviewListInfo(
                presentation.getId(),
                DateFormatUtil.formatPrettyDate(presentation.getCreatedDate())
            ).get());
        }
        info.setForeground(secondaryTextColor);

        NamedCollection<TagPresentation> tagGroup = presentation.getTagGroup();
        tags.setIcon(DvcsImplIcons.BranchLabel);
        tags.setVisible(tagGroup != null);

        if (tagGroup != null) {
            LazyIdeToolTip tooltip = new LazyIdeToolTip(tags, () ->
                createTitledList(tagGroup, (label, tag, idx) -> {
                    label.setText(tag.getName());
                    label.setForeground(UIUtil.getToolTipForeground());
                    Color color = tag.getColor();
                    if (color != null) {
                        label.setIcon(IconUtil.colorize(DvcsImplIcons.BranchLabel, color));
                    }
                    else {
                        label.setIcon(DvcsImplIcons.BranchLabel);
                    }
                })
            );
            getToolTipManager().setCustomTooltip(tags, tooltip);
        }
        else {
            getToolTipManager().setCustomTooltip(tags, null);
        }

        stateTextModel.setValue(presentation.getState());
        stateLabel.setVisible(presentation.getState() != null);

        ReviewListItemPresentation.Status mergeableStatus = presentation.getMergeableStatus();
        nonMergeable.setIcon(mergeableStatus != null ? mergeableStatus.getIcon() : null);
        nonMergeable.setToolTipText(mergeableStatus != null ? mergeableStatus.getTooltip() : null);
        nonMergeable.setVisible(mergeableStatus != null);

        ReviewListItemPresentation.Status buildStatusVal = presentation.getBuildStatus();
        buildStatus.setIcon(buildStatusVal != null ? buildStatusVal.getIcon() : null);
        buildStatus.setToolTipText(buildStatusVal != null ? buildStatusVal.getTooltip() : null);
        buildStatus.setVisible(buildStatusVal != null);

        showUsersIcon(userGroup1, presentation.getUserGroup1());
        showUsersIcon(userGroup2, presentation.getUserGroup2());

        ReviewListItemPresentation.CommentsCounter counter = presentation.getCommentsCounter();
        comments.setIcon(CollaborationToolsIcons.Comment);
        comments.setText(counter != null ? String.valueOf(counter.getCount()) : null);
        comments.setToolTipText(counter != null ? counter.getTooltip() : null);
        comments.setVisible(counter != null);
        comments.setBorder(JBUI.Borders.emptyRight(1));

        updateRendering();

        return this;
    }

    private <E> @Nonnull JComponent createTitledList(
        @Nonnull NamedCollection<E> collection,
        @Nonnull SimpleListCellRenderer.Customizer<E> customizer
    ) {
        JLabel titleLabel = new JLabel();
        titleLabel.setFont(JBUI.Fonts.smallFont());
        titleLabel.setForeground(UIUtil.getContextHelpForeground());
        titleLabel.setText(collection.getNamePlural());
        titleLabel.setBorder(JBUI.Borders.empty(0, 10, 4, 0));

        JBList<E> listComponent = new JBList<>(collection.getItems());
        listComponent.setOpaque(false);
        listComponent.setCellRenderer(SimpleListCellRenderer.create(customizer));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(listComponent, BorderLayout.CENTER);
        return panel;
    }

    private void showUsersIcon(@Nonnull JLabel label, @Nullable NamedCollection<UserPresentation> users) {
        List<Icon> icons = null;
        if (users != null) {
            List<Icon> allIcons = users.getItems().stream()
                .map(UserPresentation::getAvatarIcon)
                .limit(MAX_PARTICIPANT_ICONS)
                .collect(Collectors.toList());
            if (!allIcons.isEmpty()) {
                icons = allIcons;
            }
        }
        if (icons == null) {
            label.setVisible(false);
            label.setIcon(null);
        }
        else {
            label.setVisible(true);
            label.setIcon(new OverlaidOffsetIconsIcon(icons));
        }

        if (users != null) {
            LazyIdeToolTip tooltip = new LazyIdeToolTip(label, () ->
                createTitledList(users, (labelComp, user, idx) -> {
                    labelComp.setText(user.getPresentableName());
                    labelComp.setIcon(user.getAvatarIcon());
                    labelComp.setForeground(UIUtil.getToolTipForeground());
                })
            );
            getToolTipManager().setCustomTooltip(label, tooltip);
        }
    }

    private static boolean isLabelTruncated(@Nonnull JLabel label) {
        FontMetrics fm = label.getFontMetrics(label.getFont());
        String text = label.getText();
        return fm.stringWidth(text) > label.getWidth();
    }

    private static void addTruncationListener(@Nonnull JLabel label) {
        label.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (isLabelTruncated(label)) {
                    label.setToolTipText(label.getText());
                }
                else {
                    label.setToolTipText(null);
                }
            }
        });
    }

    /**
     * Lays out the components horizontally in two groups - {@link SwingConstants#LEFT} and {@link SwingConstants#RIGHT}
     * anchored to the left and right sides respectively.
     * Respects the minimal sizes and does not force the components to grow.
     */
    private static final class HorizontalSidesLayout extends AbstractLayoutManager {
        private final JBValue gap;
        private final List<Component> leftComponents = new ArrayList<>();
        private final List<Component> rightComponents = new ArrayList<>();

        HorizontalSidesLayout(int gapValue) {
            this.gap = new JBValue.UIInteger("", Math.max(0, gapValue));
        }

        @Override
        public void addLayoutComponent(@Nonnull Component comp, @Nullable Object constraints) {
            if (constraints instanceof Integer && (Integer) constraints == SwingConstants.RIGHT) {
                rightComponents.add(comp);
            }
            else {
                leftComponents.add(comp);
            }
        }

        @Override
        public void addLayoutComponent(@Nullable String name, @Nonnull Component comp) {
            addLayoutComponent(comp, SwingConstants.LEFT);
        }

        @Override
        public void removeLayoutComponent(@Nonnull Component comp) {
            leftComponents.remove(comp);
            rightComponents.remove(comp);
        }

        @Override
        public @Nonnull Dimension minimumLayoutSize(@Nonnull Container parent) {
            List<Component> all = new ArrayList<>(leftComponents);
            all.addAll(rightComponents);
            return getSize(all, Component::getMinimumSize);
        }

        @Override
        public @Nonnull Dimension preferredLayoutSize(@Nonnull Container parent) {
            List<Component> all = new ArrayList<>(leftComponents);
            all.addAll(rightComponents);
            return getSize(all, Component::getPreferredSize);
        }

        private @Nonnull Dimension getSize(@Nonnull List<Component> components, @Nonnull Function<Component, Dimension> dimensionGetter) {
            List<Component> visibleComponents = components.stream().filter(Component::isVisible).collect(Collectors.toList());
            Dimension dimension = new Dimension();
            for (Component component : visibleComponents) {
                Dimension size = dimensionGetter.apply(component);
                dimension.width += size.width;
                dimension.height = Math.max(dimension.height, size.height);
            }
            dimension.width += gap.get() * Math.max(0, visibleComponents.size() - 1);
            return dimension;
        }

        @Override
        public void layoutContainer(@Nonnull Container parent) {
            Rectangle bounds = new Rectangle(new Point(0, 0), parent.getSize());
            JBInsets.removeFrom(bounds, parent.getInsets());
            int height = bounds.height;

            float widthDeltaFraction = getWidthDeltaFraction(
                minimumLayoutSize(parent).width,
                preferredLayoutSize(parent).width,
                bounds.width
            );

            int leftMinWidth = getSize(leftComponents, Component::getMinimumSize).width;
            int leftPrefWidth = getSize(leftComponents, Component::getPreferredSize).width;
            int leftWidth = leftMinWidth + (int) ((leftPrefWidth - leftMinWidth) * widthDeltaFraction);

            float leftWidthDeltaFraction = getWidthDeltaFraction(leftMinWidth, leftPrefWidth, leftWidth);
            layoutGroup(leftComponents, bounds.getLocation(), height, leftWidthDeltaFraction);

            int rightMinWidth = getSize(rightComponents, Component::getMinimumSize).width;
            int rightPrefWidth = getSize(rightComponents, Component::getPreferredSize).width;
            int rightWidth = Math.min(bounds.width - leftWidth - gap.get(), rightPrefWidth);

            int rightX = bounds.x + Math.max(leftWidth + gap.get(), bounds.width - rightWidth);
            float rightWidthDeltaFraction = getWidthDeltaFraction(rightMinWidth, rightPrefWidth, rightWidth);
            layoutGroup(rightComponents, new Point(rightX, bounds.y), height, rightWidthDeltaFraction);
        }

        private static float getWidthDeltaFraction(int minWidth, int prefWidth, int currentWidth) {
            if (prefWidth <= minWidth) {
                return 0f;
            }
            float fraction = (currentWidth - minWidth) / (float) (prefWidth - minWidth);
            return Math.min(1f, Math.max(0f, fraction));
        }

        private void layoutGroup(
            @Nonnull List<Component> components, @Nonnull Point startPoint,
            int height, float groupWidthDeltaFraction
        ) {
            int x = startPoint.x;
            for (Component it : components) {
                if (!it.isVisible()) {
                    continue;
                }
                Dimension minSize = it.getMinimumSize();
                Dimension prefSize = it.getPreferredSize();
                int width = minSize.width + (int) ((prefSize.width - minSize.width) * groupWidthDeltaFraction);
                Dimension size = new Dimension(width, Math.min(prefSize.height, height));

                int y = startPoint.y + (height - size.height) / 2;
                Point location = new Point(x, y);

                it.setBounds(new Rectangle(location, size));
                x += size.width + gap.get();
            }
        }
    }

    private static final class LazyIdeToolTip extends IdeTooltip {
        private final Supplier<JComponent> tipFactory;

        LazyIdeToolTip(@Nonnull JComponent component, @Nonnull Supplier<JComponent> tipFactory) {
            super(component, new Point(0, 0), null, component);
            this.tipFactory = tipFactory;
            setToCenter(true);
            setLayer(Balloon.Layer.top);
            setPreferredPosition(Balloon.Position.atRight);
        }

        @Override
        public boolean beforeShow() {
            setTipComponent(tipFactory.get());
            return true;
        }
    }
}
