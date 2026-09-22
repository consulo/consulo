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
package consulo.externalTool.impl.internal;

import consulo.externalTool.impl.internal.localize.ExternalToolLocalize;
import consulo.util.xml.serializer.JDOMExternalizable;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author dyoma
 */
public class FilterInfo implements JDOMExternalizable {
    private static final String FILTER_NAME = "NAME";
    private static final String FILTER_DESCRIPTION = "DESCRIPTION";
    private static final String FILTER_REGEXP = "REGEXP";

    private String myName = ExternalToolLocalize.toolsFiltersNameDefault().get();
    private String myDescription;
    private String myRegExp;
    private static final String ELEMENT_OPTION = "option";
    private static final String ATTRIBUTE_VALUE = "value";
    private static final String ATTRIBUTE_NAME = "name";

    public FilterInfo() {
    }

    public FilterInfo(String regExp, String name, String description) {
        myRegExp = regExp;
        myName = name;
        myDescription = description;
    }

    public String getDescription() {
        return myDescription;
    }

    public void setDescription(String description) {
        myDescription = description;
    }

    public String getName() {
        return myName;
    }

    public void setName(String name) {
        myName = name;
    }

    public String getRegExp() {
        return myRegExp;
    }

    public void setRegExp(String regExp) {
        myRegExp = regExp;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Objects.hashCode(myName) + Objects.hashCode(myDescription)) + Objects.hashCode(myRegExp);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        return object instanceof FilterInfo that
            && Objects.equals(myName, that.myName)
            && Objects.equals(myDescription, that.myDescription)
            && Objects.equals(myRegExp, that.myRegExp);
    }

    public FilterInfo createCopy() {
        return new FilterInfo(myRegExp, myName, myDescription);
    }

    @Override
    public void readExternal(Element element) {
        for (Element optionElement : element.getChildren(ELEMENT_OPTION)) {
            String value = optionElement.getAttributeValue(ATTRIBUTE_VALUE);
            String name = optionElement.getAttributeValue(ATTRIBUTE_NAME);

            if (FILTER_NAME.equals(name)) {
                if (value != null) {
                    myName = convertString(value);
                }
            }
            if (FILTER_DESCRIPTION.equals(name)) {
                myDescription = convertString(value);
            }
            if (FILTER_REGEXP.equals(name)) {
                myRegExp = convertString(value);
            }
        }
    }

    @Override
    public void writeExternal(Element filterElement) {
        Element option = new Element(ELEMENT_OPTION);
        filterElement.addContent(option);
        option.setAttribute(ATTRIBUTE_NAME, FILTER_NAME);
        if (myName != null) {
            option.setAttribute(ATTRIBUTE_VALUE, myName);
        }

        option = new Element(ELEMENT_OPTION);
        filterElement.addContent(option);
        option.setAttribute(ATTRIBUTE_NAME, FILTER_DESCRIPTION);
        if (myDescription != null) {
            option.setAttribute(ATTRIBUTE_VALUE, myDescription);
        }

        option = new Element(ELEMENT_OPTION);
        filterElement.addContent(option);
        option.setAttribute(ATTRIBUTE_NAME, FILTER_REGEXP);
        if (myRegExp != null) {
            option.setAttribute(ATTRIBUTE_VALUE, myRegExp);
        }
    }

    public static String convertString(String s) {
        return ToolManager.convertString(s);
    }
}
