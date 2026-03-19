// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.runAnything;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class RunAnythingHelpItem extends RunAnythingItemBase {
    
    private final String myPlaceholder;
    private final @Nullable String myDescription;
    private final @Nullable Image myIcon;

    public RunAnythingHelpItem(String placeholder, String command, @Nullable String description, @Nullable Image icon) {
        super(command, icon);
        myPlaceholder = placeholder;
        myDescription = description;
        myIcon = icon;
    }

    
    @Override
    public Component createComponent(@Nullable String pattern, boolean isSelected, boolean hasFocus) {
        JPanel component = (JPanel)super.createComponent(pattern, isSelected, hasFocus);

        SimpleColoredComponent simpleColoredComponent = new SimpleColoredComponent();
        parseAndApplyStyleToParameters(simpleColoredComponent, myPlaceholder);
        appendDescription(simpleColoredComponent, myDescription, UIUtil.getListForeground(isSelected, true));
        setupIcon(simpleColoredComponent, myIcon);

        component.add(simpleColoredComponent, BorderLayout.WEST);

        return component;
    }

    private static void parseAndApplyStyleToParameters(SimpleColoredComponent component, String placeholder) {
        int lt = StringUtil.indexOf(placeholder, "<");
        if (lt == -1) {
            component.append(placeholder);
            return;
        }

        int gt = StringUtil.indexOf(placeholder, ">", lt);
        //appends leading
        component.append(gt > -1 ? placeholder.substring(0, lt) : placeholder);
        while (lt > -1 && gt > -1) {
            component.append(placeholder.substring(lt, gt + 1), SimpleTextAttributes.GRAY_ATTRIBUTES);

            lt = StringUtil.indexOf(placeholder, "<", gt);
            if (lt == -1) {
                component.append(placeholder.substring(gt + 1));
                break;
            }

            component.append(placeholder.substring(gt + 1, lt));
            gt = StringUtil.indexOf(placeholder, ">", lt);
        }
    }
}