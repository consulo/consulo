// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.ui.navigation;

import consulo.application.ui.UISettings;
import consulo.application.util.matcher.Matcher;
import consulo.application.util.matcher.MatcherHolder;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.fileEditor.VfsPresentationUtil;
import consulo.language.editor.ui.PsiElementModuleRenderer;
import consulo.language.editor.util.NavigationItemFileStatus;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementNavigationItem;
import consulo.language.psi.PsiUtilCore;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.OpaquePanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.ui.image.Image;
import consulo.ui.style.StandardColors;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

public class NavigationItemListCellRenderer extends OpaquePanel implements ListCellRenderer<Object> {
    public NavigationItemListCellRenderer() {
        super(new BorderLayout());
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        removeAll();

        boolean hasRightRenderer = UISettings.getInstance().getShowIconInQuickNavigation();

        LeftRenderer left = new LeftRenderer(true, MatcherHolder.getAssociatedMatcher(list));
        Component leftCellRendererComponent = left.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        Color listBg = leftCellRendererComponent.getBackground();
        add(leftCellRendererComponent, BorderLayout.WEST);

        setBackground(isSelected ? UIUtil.getListSelectionBackground(true) : listBg);

        if (hasRightRenderer) {
            DefaultListCellRenderer moduleRenderer = new PsiElementModuleRenderer();

            Component rightCellRendererComponent =
                moduleRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ((JComponent)rightCellRendererComponent).setOpaque(false);
            rightCellRendererComponent.setBackground(listBg);
            add(rightCellRendererComponent, BorderLayout.EAST);
            JPanel spacer = new NonOpaquePanel();

            Dimension size = rightCellRendererComponent.getSize();
            spacer.setSize(new Dimension((int)(size.width * 0.015 + leftCellRendererComponent.getSize().width * 0.015), size.height));
            spacer.setBackground(isSelected ? UIUtil.getListSelectionBackground(true) : listBg);
            add(spacer, BorderLayout.CENTER);
        }
        return this;
    }

    public static PsiElement getPsiElement(Object o) {
        return o instanceof PsiElement element ? element
            : o instanceof PsiElementNavigationItem navItem ? navItem.getTargetElement() : null;
    }

    private static class LeftRenderer extends ColoredListCellRenderer {
        public final boolean myRenderLocation;
        private final Matcher myMatcher;

        LeftRenderer(boolean renderLocation, Matcher matcher) {
            myRenderLocation = renderLocation;
            myMatcher = matcher;
        }

        @Override
        protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
            Color bgColor = UIUtil.getListBackground();

            if (value instanceof PsiElement element && !element.isValid()) {
                setIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
                append("Invalid", SimpleTextAttributes.ERROR_ATTRIBUTES);
            }
            else if (value instanceof NavigationItem item) {
                ItemPresentation presentation = item.getPresentation();
                assert presentation != null
                    : "PSI elements displayed in choose by name lists must return a non-null value from getPresentation(): " +
                    "element " + item.toString() + ", class " + item.getClass().getName();
                String name = presentation.getPresentableText();
                assert name != null : "PSI elements displayed in choose by name lists must return a non-null value " +
                    "from getPresentation().getPresentableName: element " + item.toString() + ", class " + item.getClass().getName();
                ColorValue color = TargetAWT.from(list.getForeground());
                boolean isProblemFile = item instanceof PsiElement element
                    && WolfTheProblemSolver.getInstance(element.getProject()).isProblemFile(PsiUtilCore.getVirtualFile(element));

                PsiElement psiElement = getPsiElement(item);

                if (psiElement != null && psiElement.isValid()) {
                    Project project = psiElement.getProject();

                    VirtualFile virtualFile = PsiUtilCore.getVirtualFile(psiElement);
                    isProblemFile = WolfTheProblemSolver.getInstance(project).isProblemFile(virtualFile);

                    ColorValue fileColor = virtualFile == null ? null : VfsPresentationUtil.getFileBackgroundColor(project, virtualFile);
                    if (fileColor != null) {
                        bgColor = TargetAWT.to(fileColor);
                    }
                }

                FileStatus status = NavigationItemFileStatus.get(item);
                if (status != FileStatus.NOT_CHANGED) {
                    color = status.getColor();
                }

                TextAttributes textAttributes =
                    TextAttributesUtil.toTextAttributes(NodeRenderer.getSimpleTextAttributes(presentation));
                if (isProblemFile) {
                    textAttributes.setEffectType(EffectType.WAVE_UNDERSCORE);
                    textAttributes.setEffectColor(StandardColors.RED);
                }
                textAttributes.setForegroundColor(color);
                SimpleTextAttributes nameAttributes = TextAttributesUtil.fromTextAttributes(textAttributes);
                SpeedSearchUtil.appendColoredFragmentForMatcher(name, this, nameAttributes, myMatcher, bgColor, selected);
                setIcon(presentation.getIcon());

                if (myRenderLocation) {
                    String containerText = presentation.getLocationString();

                    if (containerText != null && containerText.length() > 0) {
                        append(" " + containerText, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY));
                    }
                }
            }
            else {
                setIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
                append(
                    value == null ? "" : value.toString(),
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, list.getForeground())
                );
            }
            setBackground(selected ? UIUtil.getListSelectionBackground(true) : bgColor);
        }
    }
}
