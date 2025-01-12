// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.ui;

import consulo.application.ReadAction;
import consulo.application.ui.UISettings;
import consulo.application.util.matcher.Matcher;
import consulo.application.util.matcher.MatcherHolder;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.component.util.Iconable;
import consulo.fileEditor.VfsPresentationUtil;
import consulo.language.editor.ui.navigation.NavigationItemListCellRenderer;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.logging.Logger;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.ColoredItemPresentation;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.regex.Pattern;

public abstract class PsiElementListCellRenderer<T extends PsiElement> extends JPanel implements ListCellRenderer<T> {
    private static final Logger LOG = Logger.getInstance(PsiElementListCellRenderer.class);
    private static final String LEFT = BorderLayout.WEST;
    private static final Pattern CONTAINER_PATTERN = Pattern.compile("(\\(in |\\()?([^)]*)(\\))?");

    protected int myRightComponentWidth;

    protected PsiElementListCellRenderer() {
        super(new BorderLayout());
        setBorder(JBCurrentTheme.listCellBorder());
    }

    private class MyAccessibleContext extends JPanel.AccessibleJPanel {
        @Override
        public String getAccessibleName() {
            LayoutManager lm = getLayout();
            assert lm instanceof BorderLayout;
            Component leftCellRendererComp = ((BorderLayout)lm).getLayoutComponent(LEFT);
            return leftCellRendererComp instanceof Accessible
                ? leftCellRendererComp.getAccessibleContext().getAccessibleName()
                : super.getAccessibleName();
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new MyAccessibleContext();
        }
        return accessibleContext;
    }

    @Nullable
    protected static ColorValue getBackgroundColor(@Nullable Object value) {
        PsiElement psiElement = NavigationItemListCellRenderer.getPsiElement(value);
        VirtualFile virtualFile = PsiUtilCore.getVirtualFile(psiElement);
        ColorValue fileColor = virtualFile == null ? null : VfsPresentationUtil.getFileBackgroundColor(psiElement.getProject(), virtualFile);
        return fileColor != null ? fileColor : null;
    }

    public static class ItemMatchers {
        @Nullable
        public final Matcher nameMatcher;
        @Nullable
        final Matcher locationMatcher;

        public ItemMatchers(@Nullable Matcher nameMatcher, @Nullable Matcher locationMatcher) {
            this.nameMatcher = nameMatcher;
            this.locationMatcher = locationMatcher;
        }
    }

    private class LeftRenderer extends ColoredListCellRenderer {
        private final String myModuleName;
        private final ItemMatchers myMatchers;

        LeftRenderer(final String moduleName, @Nonnull ItemMatchers matchers) {
            myModuleName = moduleName;
            myMatchers = matchers;
        }

        @Override
        protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
            Color bgColor = UIUtil.getListBackground();
            Color color = list.getForeground();

            PsiElement target = NavigationItemListCellRenderer.getPsiElement(value);
            VirtualFile vFile = PsiUtilCore.getVirtualFile(target);
            boolean isProblemFile = false;
            if (vFile != null) {
                Project project = target.getProject();
                isProblemFile = WolfTheProblemSolver.getInstance(project).isProblemFile(vFile);
                FileStatus status = FileStatusManager.getInstance(project).getStatus(vFile);
                color = TargetAWT.to(status.getColor());

                ColorValue fileBgColor = VfsPresentationUtil.getFileBackgroundColor(project, vFile);
                bgColor = fileBgColor == null ? bgColor : TargetAWT.to(fileBgColor);
            }

            if (value instanceof PsiElement) {
                T element = (T)value;
                String name = ((PsiElement)value).isValid() ? getElementText(element) : "INVALID";

                TextAttributes attributes = element.isValid() ? getNavigationItemAttributes(value) : null;
                SimpleTextAttributes nameAttributes = attributes != null ? TextAttributesUtil.fromTextAttributes(attributes) : null;
                if (nameAttributes == null) {
                    nameAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
                }

                if (name == null) {
                    LOG.error("Null name for PSI element " + element.getClass() + " (by " + PsiElementListCellRenderer.this + ")");
                    name = "Unknown";
                }
                SpeedSearchUtil.appendColoredFragmentForMatcher(name, this, nameAttributes, myMatchers.nameMatcher, bgColor, selected);
                if (!element.isValid()) {
                    append(" Invalid", SimpleTextAttributes.ERROR_ATTRIBUTES);
                    return;
                }
                setIcon(PsiElementListCellRenderer.this.getIcon(element));

                FontMetrics fm = list.getFontMetrics(list.getFont());
                int maxWidth = list.getWidth() - fm.stringWidth(name) -
                    (myModuleName != null ? fm.stringWidth(myModuleName + "        ") : 0) - 16 - myRightComponentWidth - 20;
                String containerText = getContainerTextForLeftComponent(element, name, maxWidth, fm);
                if (containerText != null) {
                    appendLocationText(selected, bgColor, isProblemFile, containerText);
                }
            }
            else if (!customizeNonPsiElementLeftRenderer(this, list, value, index, selected, hasFocus)) {
                setIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
                append(
                    value == null ? "" : value.toString(),
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, list.getForeground())
                );
            }
            setBackground(selected ? UIUtil.getListSelectionBackground(true) : bgColor);
        }

        private void appendLocationText(boolean selected, Color bgColor, boolean isProblemFile, String containerText) {
            SimpleTextAttributes locationAttrs = SimpleTextAttributes.GRAYED_ATTRIBUTES;
            if (isProblemFile) {
                SimpleTextAttributes wavedAttributes = SimpleTextAttributes.merge(
                    new SimpleTextAttributes(
                        SimpleTextAttributes.STYLE_WAVED,
                        UIUtil.getInactiveTextColor(),
                        JBColor.RED
                    ),
                    locationAttrs
                );
                java.util.regex.Matcher matcher = CONTAINER_PATTERN.matcher(containerText);
                if (matcher.matches()) {
                    String prefix = matcher.group(1);
                    SpeedSearchUtil.appendColoredFragmentForMatcher(
                        " " + ObjectUtil.notNull(prefix, ""),
                        this,
                        locationAttrs,
                        myMatchers.locationMatcher,
                        bgColor,
                        selected
                    );

                    String strippedContainerText = matcher.group(2);
                    SpeedSearchUtil.appendColoredFragmentForMatcher(
                        ObjectUtil.notNull(strippedContainerText, ""),
                        this,
                        wavedAttributes,
                        myMatchers.locationMatcher,
                        bgColor,
                        selected
                    );

                    String suffix = matcher.group(3);
                    if (suffix != null) {
                        SpeedSearchUtil.appendColoredFragmentForMatcher(
                            suffix,
                            this,
                            locationAttrs,
                            myMatchers.locationMatcher,
                            bgColor,
                            selected
                        );
                    }
                    return;
                }
                locationAttrs = wavedAttributes;
            }
            SpeedSearchUtil.appendColoredFragmentForMatcher(
                " " + containerText,
                this,
                locationAttrs,
                myMatchers.locationMatcher,
                bgColor,
                selected
            );
        }
    }

    @Nullable
    protected TextAttributes getNavigationItemAttributes(Object value) {
        TextAttributes attributes = null;

        if (value instanceof NavigationItem) {
            TextAttributesKey attributesKey = null;
            final ItemPresentation presentation = ((NavigationItem)value).getPresentation();
            if (presentation instanceof ColoredItemPresentation) {
                attributesKey = ((ColoredItemPresentation)presentation).getTextAttributesKey();
            }

            if (attributesKey != null) {
                attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(attributesKey);
            }
        }
        return attributes;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
        removeAll();
        myRightComponentWidth = 0;
        DefaultListCellRenderer rightRenderer = getRightCellRenderer(value);
        Component rightCellRendererComponent = null;
        JPanel spacer = null;
        if (rightRenderer != null) {
            rightCellRendererComponent = rightRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            add(rightCellRendererComponent, BorderLayout.EAST);
            spacer = new JPanel();
            spacer.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            add(spacer, BorderLayout.CENTER);
            myRightComponentWidth = rightCellRendererComponent.getPreferredSize().width;
            myRightComponentWidth += spacer.getPreferredSize().width;
        }

        ListCellRenderer leftRenderer = new LeftRenderer(
            null,
            value == null ? new ItemMatchers(null, null) : getItemMatchers(list, value)
        );
        final Component leftCellRendererComponent = leftRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        add(leftCellRendererComponent, LEFT);
        final Color bg = isSelected ? UIUtil.getListSelectionBackground(true) : leftCellRendererComponent.getBackground();
        setBackground(bg);
        if (rightCellRendererComponent != null) {
            rightCellRendererComponent.setBackground(bg);
        }
        if (spacer != null) {
            spacer.setBackground(bg);
        }
        return this;
    }

    @Nonnull
    protected ItemMatchers getItemMatchers(@Nonnull JList list, @Nonnull Object value) {
        return new ItemMatchers(MatcherHolder.getAssociatedMatcher(list), null);
    }

    protected boolean customizeNonPsiElementLeftRenderer(
        ColoredListCellRenderer renderer,
        JList list,
        Object value,
        int index,
        boolean selected,
        boolean hasFocus
    ) {
        return false;
    }

    @Nullable
    protected DefaultListCellRenderer getRightCellRenderer(final Object value) {
        if (UISettings.getInstance().SHOW_ICONS_IN_QUICK_NAVIGATION) {
            return new PsiElementModuleRenderer();
        }
        return null;
    }

    public abstract String getElementText(T element);

    @Nullable
    protected abstract String getContainerText(T element, final String name);

    @Nullable
    protected String getContainerTextForLeftComponent(T element, String name, int maxWidth, FontMetrics fm) {
        return getContainerText(element, name);
    }

    @Iconable.IconFlags
    protected abstract int getIconFlags();

    protected Image getIcon(PsiElement element) {
        return IconDescriptorUpdaters.getIcon(element, getIconFlags());
    }

    public Comparator<T> getComparator() {
        //noinspection unchecked
        return Comparator.comparing(this::getComparingObject);
    }

    @Nonnull
    public Comparable getComparingObject(T element) {
        return ReadAction.compute(() -> {
            String elementText = getElementText(element);
            String containerText = getContainerText(element, elementText);
            return containerText == null ? elementText : elementText + " " + containerText;
        });
    }

    public void installSpeedSearch(IPopupChooserBuilder builder) {
        installSpeedSearch(builder, false);
    }

    public void installSpeedSearch(IPopupChooserBuilder builder, final boolean includeContainerText) {
        builder.setNamerForFiltering(o -> {
            if (o instanceof PsiElement) {
                final String elementText = getElementText((T)o);
                if (includeContainerText) {
                    return elementText + " " + getContainerText((T)o, elementText);
                }
                return elementText;
            }
            else {
                return o.toString();
            }
        });
    }
}
