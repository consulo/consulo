// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.uiOld.components.fields;

import consulo.application.AllIcons;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Arrays.asList;

/**
 * @author Sergey Malenkov
 */
public class ExtendableTextField extends JBTextField implements ExtendableTextComponent {
    private List<Extension> extensions = List.of();

    public ExtendableTextField() {
        this(null);
    }

    public ExtendableTextField(int columns) {
        this(null, columns);
    }

    public ExtendableTextField(String text) {
        this(text, 20);
    }

    public ExtendableTextField(String text, int columns) {
        super(text, columns);
    }

    @Override
    public List<Extension> getExtensions() {
        return extensions;
    }

    @Override
    public void setExtensions(Extension... extensions) {
        setExtensions(asList(extensions));
    }

    @Override
    public void setExtensions(Collection<? extends Extension> extensions) {
        setExtensions(new ArrayList<>(extensions));
    }

    private void setExtensions(List<? extends Extension> extensions) {
        List<Extension> left = List.of();
        List<Extension> right = List.of();

        for (Extension extension : extensions.reversed()) {
            if (extension.isIconBeforeText()) {
                if (left.isEmpty()) {
                    left = new ArrayList<>();
                }

                left.add(extension);
            } else {
                if (right.isEmpty()) {
                    right = new ArrayList<>();
                }

                right.add(extension);
            }
        }

        setUIExtensions(left, "JTextField.leadingComponent").ifPresent(it -> it.setBorder(JBCurrentTheme.textFieldSubBorder(true)));

        setUIExtensions(right, "JTextField.trailingComponent").ifPresent(it -> it.setBorder(JBCurrentTheme.textFieldSubBorder(false)));

        this.extensions = Collections.unmodifiableList(extensions);
    }

    private Optional<JToolBar> setUIExtensions(List<? extends Extension> extensions, String clientPropertyName) {
        if (extensions.isEmpty()) {
            putClientProperty(clientPropertyName, null);
            return Optional.empty();
        }
        else {
            JToolBar toolBar = new JToolBar();

            for (Extension extension : extensions) {
                Consumer<AWTEvent> actionOnClick = extension.getActionOnClick();

                JComponent component;
                if (actionOnClick == null) {
                    component = new JBLabel(extension.getIcon(false));
                    component.setBorder(JBUI.Borders.empty(3));
                }
                else {
                    JButton button = new JButton(TargetAWT.to(extension.getIcon(false)));
                    button.setRolloverIcon(TargetAWT.to(extension.getIcon(true)));
                    button.addActionListener(actionOnClick::accept);

                    component = button;
                }
                toolBar.add(component);
            }
            putClientProperty(clientPropertyName, toolBar);
            return Optional.of(toolBar);
        }
    }

    @Override
    public void addExtension(@Nonnull Extension extension) {
        if (!getExtensions().contains(extension)) {
            List<Extension> extensions = new ArrayList<>(getExtensions());
            extensions.add(extension);
            setExtensions(extensions);
        }
    }

    @Override
    public void removeExtension(@Nonnull Extension extension) {
        ArrayList<Extension> extensions = new ArrayList<>(getExtensions());
        if (extensions.remove(extension)) {
            setExtensions(extensions);
        }
    }

    public ExtendableTextField addBrowseExtension(@Nonnull Runnable action, @Nullable Disposable parentDisposable) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
        String tooltip = UIBundle.message("component.with.browse.button.browse.button.tooltip.text") + " (" + KeymapUtil.getKeystrokeText(keyStroke) + ")";

        ExtendableTextComponent.Extension browseExtension = ExtendableTextComponent.Extension.create(AllIcons.Nodes.TreeOpen, AllIcons.Nodes.TreeOpen, tooltip, action);

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                action.run();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(keyStroke), this, parentDisposable);
        addExtension(browseExtension);

        return this;
    }
}
