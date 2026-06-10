// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.TextAttributesKey;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInsight.hints.InlayHintsUtils;
import consulo.ide.impl.idea.codeInsight.hints.InlayTextMetricsStorage;
import consulo.ide.impl.idea.codeInsight.hints.InsetValueProvider;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.InlayPresentationFactory;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.editor.ui.internal.HintManagerEx;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.hint.LightweightHintFactory;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.util.io.URLUtil;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.archive.ArchiveFileSystem;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PresentationFactory implements InlayPresentationFactory {
    private final Editor editor;
    private final InlayTextMetricsStorage textMetricsStorage;
    private final InsetValueProvider offsetFromTopProvider;

    public PresentationFactory(Editor editor) {
        this.editor = editor;
        this.textMetricsStorage = InlayHintsUtils.getTextMetricStorage(editor);
        this.offsetFromTopProvider = new InsetValueProvider() {
            @Override
            @RequiredUIAccess
            public int getTop() {
                return textMetricsStorage.getFontMetrics(true).offsetFromTop();
            }
        };
    }

    @Override
    public InlayPresentation smallText(String text) {
        InlayPresentation textWithoutBox = new InsetPresentation(new TextInlayPresentation(textMetricsStorage, true, text), 0, 0, 1, 1);
        return withInlayAttributes(textWithoutBox);
    }

    public InlayPresentation smallTextWithoutBackground(String text) {
        InlayPresentation textWithoutBox = new InsetPresentation(new TextInlayPresentation(textMetricsStorage, true, text), 0, 0, 1, 1);
        return new WithAttributesPresentation(
            textWithoutBox,
            DefaultLanguageHighlighterColors.INLAY_TEXT_WITHOUT_BACKGROUND,
            editor,
            new WithAttributesPresentation.AttributesFlags().withIsDefault(true)
        );
    }

    @Override
    public InlayPresentation container(
        InlayPresentation presentation,
        Padding padding,
        RoundedCorners roundedCorners,
        ColorValue background,
        float backgroundAlpha
    ) {
        return new ContainerInlayPresentation(presentation, padding, roundedCorners, background, backgroundAlpha);
    }

    @Override
    public InlayPresentation mouseHandling(InlayPresentation base, ClickListener clickListener, HoverListener hoverListener) {
        return new MouseHandlingPresentation(base, clickListener, hoverListener);
    }

    public InlayPresentation textSpacePlaceholder(int length, boolean isSmall) {
        return new TextPlaceholderPresentation(length, textMetricsStorage, isSmall);
    }

    @Override
    public InlayPresentation text(String text) {
        return withInlayAttributes(new TextInlayPresentation(textMetricsStorage, false, text));
    }

    public InlayPresentation roundWithBackground(InlayPresentation base) {
        InlayPresentation rounding = new WithAttributesPresentation(
            new RoundWithBackgroundPresentation(new InsetPresentation(base, 7, 7, 0, 0), 8, 8),
            DefaultLanguageHighlighterColors.INLAY_DEFAULT,
            editor,
            new WithAttributesPresentation.AttributesFlags().withIsDefault(true)
        );
        return new DynamicInsetPresentation(rounding, offsetFromTopProvider);
    }

    public InlayPresentation roundWithBackgroundAndSmallInset(InlayPresentation base) {
        InlayPresentation rounding = new WithAttributesPresentation(
            new RoundWithBackgroundPresentation(new InsetPresentation(base, 3, 3, 0, 0), 8, 8),
            DefaultLanguageHighlighterColors.INLAY_DEFAULT,
            editor,
            new WithAttributesPresentation.AttributesFlags().withIsDefault(true)
        );
        return new DynamicInsetPresentation(rounding, offsetFromTopProvider);
    }

    public InlayPresentation opaqueBorderedRoundWithBackgroundAndSmallInset(InlayPresentation base, ColorValue borderColor) {
        RoundWithBackgroundPresentation inner = new RoundWithBackgroundPresentation(
            new InsetPresentation(base, base.getHeight() / 2, base.getHeight() / 2, 0, 0),
            base.getHeight(),
            base.getHeight(),
            editor.getColorsScheme().getDefaultBackground(),
            1f
        );
        InlayPresentation rounded = new RoundWithBackgroundBorderedPresentation(inner, borderColor, 1);
        WithAttributesPresentation withAttr = new WithAttributesPresentation(
            rounded,
            DefaultLanguageHighlighterColors.INLAY_DEFAULT,
            editor,
            new WithAttributesPresentation.AttributesFlags().withIsDefault(true)
        );
        return new DynamicInsetPresentation(withAttr, offsetFromTopProvider);
    }

    public InlayPresentation roundWithBackgroundAndNoInset(InlayPresentation base) {
        InlayPresentation rounding = new WithAttributesPresentation(
            new RoundWithBackgroundPresentation(base, 8, 8),
            DefaultLanguageHighlighterColors.INLAY_DEFAULT,
            editor,
            new WithAttributesPresentation.AttributesFlags().withIsDefault(true)
        );
        return new DynamicInsetPresentation(rounding, offsetFromTopProvider);
    }

    public InlayPresentation offsetFromTopForSmallText(InlayPresentation base) {
        return new DynamicInsetPresentation(base, offsetFromTopProvider);
    }

    @Override
    public IconPresentation icon(Image icon) {
        return new IconPresentation(icon, editor.getComponent());
    }

    public InlayPresentation scaledIcon(Image icon, float scaleFactor) {
        return new ScaledIconWithCustomFactorPresentation(textMetricsStorage, false, icon, editor.getComponent(), scaleFactor);
    }

    @Override
    public InlayPresentation smallScaledIcon(Image icon) {
        InlayPresentation iconWithoutBox = new InsetPresentation(
            new ScaledIconPresentation(textMetricsStorage, true, icon, editor.getComponent()),
            0, 0, 1, 1
        );
        return withInlayAttributes(iconWithoutBox);
    }

    public InlayPresentation folding(InlayPresentation placeholder, Supplier<InlayPresentation> unwrapAction) {
        return new ChangeOnClickPresentation(
            changeOnHover(placeholder, () -> {
                attributes(placeholder, EditorColors.FOLDED_TEXT_ATTRIBUTES);
                return placeholder;
            }),
            unwrapAction
        );
    }

    public InsetPresentation inset(InlayPresentation base, int left, int right, int top, int down) {
        return new InsetPresentation(base, left, right, top, down);
    }

    public InlayPresentation collapsible(
        InlayPresentation prefix,
        InlayPresentation collapsed,
        Supplier<InlayPresentation> expanded,
        InlayPresentation suffix,
        boolean startWithPlaceholder
    ) {
        SimpleReference<BiStatePresentation> presentationToChange = SimpleReference.create();

        Pair<InlayPresentation, InlayPresentation> braces = matchingBraces(prefix, suffix);

        BiStatePresentation content = new BiStatePresentation(
            () -> onClick(collapsed, MouseButton.Left, (event, translated) -> {
                BiStatePresentation biStatePresentation = presentationToChange.get();
                if (biStatePresentation != null) {
                    biStatePresentation.flipState();
                }
            }),
            expanded,
            startWithPlaceholder
        );

        presentationToChange.set(content);

        InlayPresentation leftMatching = braces.getFirst();
        InlayPresentation rightMatching = braces.getSecond();

        OnClickPresentation prefixExposed = new OnClickPresentation(leftMatching, (e, p) -> content.flipState());
        OnClickPresentation suffixExposed = new OnClickPresentation(rightMatching, (e, p) -> content.flipState());
        return seq(prefixExposed, content, suffixExposed);
    }

    public Pair<InlayPresentation, InlayPresentation> matchingBraces(InlayPresentation left, InlayPresentation right) {
        List<InlayPresentation> list = List.of(left, right);
        List<InlayPresentation> matched = matching(list);
        return Couple.of(matched.get(0), matched.get(1));
    }

    public List<InlayPresentation> matching(List<InlayPresentation> presentations) {
        List<DynamicDelegatePresentation> forwardings = new ArrayList<>();
        for (InlayPresentation p : presentations) {
            forwardings.add(new DynamicDelegatePresentation(p));
        }
        List<InlayPresentation> result = new ArrayList<>();
        for (int i = 0; i < presentations.size(); i++) {
            InlayPresentation original = presentations.get(i);
            DynamicDelegatePresentation forwarding = forwardings.get(i);
            result.add(onHover(forwarding, new HoverListener() {
                @Override
                public void onHover(MouseEvent event, Point translated) {
                    forwarding.setDelegate(attributes(original, CodeInsightColors.MATCHED_BRACE_ATTRIBUTES));
                }

                @Override
                public void onHoverFinished() {
                    forwarding.setDelegate(original);
                }
            }));
        }
        return result;
    }

    public InlayPresentation onHover(InlayPresentation base, HoverListener hoverListener) {
        return new OnHoverPresentation(base, hoverListener);
    }

    public InlayPresentation onClick(InlayPresentation base, ClickListener listener) {
        return new OnClickPresentation(base, listener);
    }

    public InlayPresentation onClick(InlayPresentation base, MouseButton button, ClickListener listener) {
        return new OnClickPresentation(base, (e, p) -> {
            if (MouseButton.getMouseButton(e) == button) {
                listener.onClick(e, p);
            }
        });
    }

    public InlayPresentation onClick(InlayPresentation base, EnumSet<MouseButton> buttons, ClickListener listener) {
        return new OnClickPresentation(base, (e, p) -> {
            if (buttons.contains(MouseButton.getMouseButton(e))) {
                listener.onClick(e, p);
            }
        });
    }

    public InlayPresentation changeOnHover(InlayPresentation base, Supplier<InlayPresentation> onHover) {
        return changeOnHover(base, onHover, e -> true);
    }

    public InlayPresentation changeOnHover(
        InlayPresentation base,
        Supplier<InlayPresentation> onHover,
        Predicate<MouseEvent> predicate
    ) {
        return new ChangeOnHoverPresentation(base, onHover, predicate);
    }

    public InlayPresentation reference(InlayPresentation base, Runnable onClickAction) {
        return referenceInternal(base, onClickAction, null);
    }

    public InlayPresentation referenceOnHover(InlayPresentation base, ClickListener clickListener) {
        InlayPresentation hovered = onClick(
            withReferenceAttributes(base),
            EnumSet.of(MouseButton.Left, MouseButton.Middle),
            clickListener
        );
        return new WithCursorOnHoverPresentation(
            new ChangeOnHoverPresentation(base, () -> hovered),
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR),
            editor
        );
    }

    private InlayPresentation referenceInternal(InlayPresentation base, Runnable onClickAction, Supplier<String> toStringProvider) {
        InlayPresentation noHighlight = onClick(base, EnumSet.of(MouseButton.Middle), (e, p) -> onClickAction.run());
        return new ChangeOnHoverPresentation(
            noHighlight,
            () -> {
                InlayPresentation withHover = onClick(
                    withReferenceAttributes(noHighlight),
                    EnumSet.of(MouseButton.Left, MouseButton.Middle),
                    (e, p) -> onClickAction.run()
                );
                return withHover;
            },
            this::isControlDown
        ) {
            @Override
            public String toString() {
                if (toStringProvider != null) {
                    return "[" + toStringProvider.get() + "]" + super.toString();
                }
                return super.toString();
            }
        };
    }

    public InlayPresentation withCursorOnHover(InlayPresentation base, Cursor cursor) {
        return new WithCursorOnHoverPresentation(base, cursor, editor);
    }

    public InlayPresentation withReferenceAttributes(InlayPresentation base) {
        return attributes(base, EditorColors.REFERENCE_HYPERLINK_COLOR);
    }

    public InlayPresentation psiSingleReference(InlayPresentation base, boolean withDebugToString, Supplier<PsiElement> resolve) {
        if (withDebugToString) {
            return referenceInternal(
                base,
                () -> navigateInternal(resolve),
                () -> {
                    PsiElement el = resolve.get();
                    return el != null ? toStringProvider().apply(el) : "";
                }
            );
        }
        else {
            return reference(base, () -> navigateInternal(resolve));
        }
    }

    public InlayPresentation psiSingleReference(InlayPresentation base, Supplier<PsiElement> resolve) {
        return reference(base, () -> navigateInternal(resolve));
    }

    public InlayPresentation seq(InlayPresentation... presentations) {
        if (presentations.length == 0) {
            return new SpacePresentation(0, 0);
        }
        if (presentations.length == 1) {
            return presentations[0];
        }
        return new SequencePresentation(Arrays.asList(presentations));
    }

    public InlayPresentation join(List<InlayPresentation> presentations, Supplier<InlayPresentation> separator) {
        List<InlayPresentation> seq = new ArrayList<>();
        boolean first = true;
        for (InlayPresentation p : presentations) {
            if (!first) {
                seq.add(separator.get());
            }
            seq.add(p);
            first = false;
        }
        return new SequencePresentation(seq);
    }

    public Pair<InlayPresentation, BiStatePresentation> button(
        InlayPresentation defaultPres,
        InlayPresentation clicked,
        ClickListener clickListener,
        HoverListener hoverListener,
        boolean initialState
    ) {
        BiStatePresentation state = new BiStatePresentation(() -> defaultPres, () -> clicked, initialState) {
            @Override
            public int getWidth() {
                return Math.max(defaultPres.getWidth(), clicked.getWidth());
            }

            @Override
            public int getHeight() {
                return Math.max(defaultPres.getHeight(), clicked.getHeight());
            }
        };
        StaticDelegatePresentation wrapper = new StaticDelegatePresentation(state) {
            @Override
            public void mouseClicked(MouseEvent e, Point p) {
                if (clickListener != null) {
                    clickListener.onClick(e, p);
                }
                state.flipState();
            }

            @Override
            public void mouseMoved(MouseEvent e, Point p) {
                if (hoverListener != null) {
                    hoverListener.onHover(e, p);
                }
            }

            @Override
            public void mouseExited() {
                if (hoverListener != null) {
                    hoverListener.onHoverFinished();
                }
            }
        };
        return new Pair<>(wrapper, state);
    }

    private WithAttributesPresentation attributes(InlayPresentation base, TextAttributesKey key) {
        return new WithAttributesPresentation(base, key, editor, new WithAttributesPresentation.AttributesFlags());
    }

    private InlayPresentation withInlayAttributes(InlayPresentation base) {
        return new WithAttributesPresentation(
            base,
            DefaultLanguageHighlighterColors.INLAY_DEFAULT,
            editor,
            new WithAttributesPresentation.AttributesFlags().withIsDefault(true)
        );
    }

    private boolean isControlDown(MouseEvent e) {
        return (Platform.current().os().isMac() && e.isMetaDown()) || e.isControlDown();
    }

    public InlayPresentation withTooltip(String tooltip, InlayPresentation base) {
        if (tooltip.isEmpty()) {
            return base;
        }
        return new OnHoverPresentation(base, new HoverListener() {
            private LightweightHint hint;

            @Override
            @RequiredUIAccess
            public void onHover(MouseEvent e, Point p) {
                if ((hint == null || !hint.isVisible()) && editor.getContentComponent().isShowing()) {
                    hint = showTooltip(e, tooltip);
                }
            }

            @Override
            public void onHoverFinished() {
                if (hint != null) {
                    hint.hide();
                    hint = null;
                }
            }
        });
    }

    @RequiredUIAccess
    public LightweightHint showTooltip(MouseEvent e, String text) {
        JComponent label = HintUtil.createInformationLabel(text);
        label.setBorder(JBUI.Borders.empty(6, 6, 5, 6));
        LightweightHint hint = Application.get().getInstance(LightweightHintFactory.class).create(label);
        Point loc = new Point(
            e.getXOnScreen() - editor.getComponent().getTopLevelAncestor().getLocationOnScreen().x,
            e.getYOnScreen() - editor.getComponent().getTopLevelAncestor().getLocationOnScreen().y
        );
        var pos = editor.xyToVisualPosition(loc);
        Point point = HintManagerImpl.getInstanceImpl().getHintPosition(hint, editor, pos, HintManager.ABOVE);
        HintManagerImpl.getInstanceImpl().showEditorHint(
            hint,
            editor,
            point,
            HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING,
            0,
            false,
            ((HintManagerEx) HintManager.getInstance())
                .createHintHint(editor, point, hint, HintManager.ABOVE)
                .setContentActive(false)
        );
        return hint;
    }

    private void navigateInternal(Supplier<PsiElement> resolve) {
        PsiElement element = resolve.get();
        if (element instanceof Navigatable navigable) {
            CommandProcessor.getInstance().newCommand()
                .project(element.getProject())
                .run(() -> navigable.navigate(true));
        }
    }

    public static Function<PsiElement, String> customToStringProvider;

    @RequiredReadAction
    private static final Function<PsiElement, String> defaultStringProvider = element -> {
        String path;
        if (element.getContainingFile().getVirtualFile().getFileSystem() instanceof ArchiveFileSystem) {
            ArchiveFileSystem fs = (ArchiveFileSystem) element.getContainingFile().getVirtualFile().getFileSystem();
            String root = ArchiveVfsUtil.getArchiveRootForLocalFile(element.getContainingFile().getVirtualFile()).getName();
            path = fs.getProtocol() + "://" + root +
                URLUtil.JAR_SEPARATOR +
                VirtualFileUtil.getRelativeLocation(
                    element.getContainingFile().getVirtualFile(),
                    ArchiveVfsUtil.getArchiveRootForLocalFile(element.getContainingFile().getVirtualFile())
                );
        }
        else {
            path = element.getContainingFile().getVirtualFile().toString();
        }
        int offset = element.getTextRange().getStartOffset();
        return path + ":" + offset;
    };

    private static Function<PsiElement, String> toStringProvider() {
        return customToStringProvider != null ? customToStringProvider : defaultStringProvider;
    }
}
