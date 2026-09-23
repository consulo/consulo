/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.psi.search;

import consulo.colorScheme.TextAttributes;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.ui.image.Image;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author Vladimir Kondratyev
 */
public class TodoAttributes implements Cloneable {
    private Image myIcon;
    private TextAttributes myTextAttributes = new TextAttributes();
    private boolean myShouldUseCustomColors;

    private static final String ATTRIBUTE_ICON = "icon";
    private static final String ICON_DEFAULT = "default";
    private static final String ICON_QUESTION = "question";
    private static final String ICON_IMPORTANT = "important";
    private static final String ELEMENT_OPTION = "option";
    private static final String USE_CUSTOM_COLORS_ATT = "useCustomColors";

    public TodoAttributes(Element element) throws InvalidDataException {
        String icon = element.getAttributeValue(ATTRIBUTE_ICON, ICON_DEFAULT);

        if (ICON_DEFAULT.equals(icon)) {
            myIcon = PlatformIconGroup.generalTododefault();
        }
        else if (ICON_QUESTION.equals(icon)) {
            myIcon = PlatformIconGroup.generalTodoquestion();
        }
        else if (ICON_IMPORTANT.equals(icon)) {
            myIcon = PlatformIconGroup.generalTodoimportant();
        }
        else {
            throw new InvalidDataException(icon);
        }

        myTextAttributes.readExternal(element);

        // default color setting
        String useCustomColors = element.getAttributeValue(USE_CUSTOM_COLORS_ATT);
        myShouldUseCustomColors = Boolean.parseBoolean(useCustomColors);

        if (element.getChild(ELEMENT_OPTION) == null) {
            myShouldUseCustomColors = false;
        }
    }

    public TodoAttributes(Image icon, TextAttributes textAttributes) {
        myIcon = icon;
        myTextAttributes = textAttributes;
    }

    public Image getIcon() {
        return myIcon;
    }

    /**
     * @see TodoAttributesUtil#getTextAttributes(TodoAttributes)
     */
    public TextAttributes getTextAttributes() {
        return myTextAttributes;
    }

    public void setIcon(Image icon) {
        myIcon = icon;
    }

    public void writeExternal(Element element) throws WriteExternalException {
        String icon;
        if (myIcon == PlatformIconGroup.generalTododefault()) {
            icon = ICON_DEFAULT;
        }
        else if (myIcon == PlatformIconGroup.generalTodoquestion()) {
            icon = ICON_QUESTION;
        }
        else if (myIcon == PlatformIconGroup.generalTodoimportant()) {
            icon = ICON_IMPORTANT;
        }
        else {
            throw new WriteExternalException("");
        }
        element.setAttribute(ATTRIBUTE_ICON, icon);
        myTextAttributes.writeExternal(element);

        // default color setting
        element.setAttribute(USE_CUSTOM_COLORS_ATT, Boolean.toString(shouldUseCustomTodoColor()));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof TodoAttributes that
            && myIcon == that.myIcon
            && Objects.equals(myTextAttributes, that.myTextAttributes)
            && myShouldUseCustomColors == that.myShouldUseCustomColors;
    }

    @Override
    public int hashCode() {
        int result = 29 * Objects.hashCode(myIcon) + Objects.hashCode(myTextAttributes);
        return 29 * result + Boolean.hashCode(myShouldUseCustomColors);
    }

    public boolean shouldUseCustomTodoColor() {
        return myShouldUseCustomColors;
    }

    public void setUseCustomTodoColor(boolean useCustomColors, TextAttributes defaultTodoAttributes) {
        myShouldUseCustomColors = useCustomColors;
        if (!useCustomColors) {
            myTextAttributes = defaultTodoAttributes;
        }
    }

    @Override
    public TodoAttributes clone() {
        try {
            TextAttributes textAttributes = myTextAttributes.clone();
            TodoAttributes attributes = (TodoAttributes) super.clone();
            attributes.myTextAttributes = textAttributes;
            attributes.myShouldUseCustomColors = myShouldUseCustomColors;
            return attributes;
        }
        catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
