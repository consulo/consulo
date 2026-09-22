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
package consulo.project.ui.view.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;

import java.util.Objects;

/**
 * @author cdr
 */
public abstract class AbstractUrl {
    protected final String url;
    protected final String moduleName;
    private final String myType;

    protected AbstractUrl(String url, String moduleName, String type) {
        myType = type;
        this.url = url == null ? "" : url;
        this.moduleName = moduleName;
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public void write(Element element) {
        element.setAttribute("url", url);
        if (moduleName != null) {
            element.setAttribute("module", moduleName);
        }
        element.setAttribute("type", myType);
    }

    @RequiredReadAction
    public abstract Object @Nullable [] createPath(Project project);

    // return null if cannot recognize the element
    public AbstractUrl createUrl(String type, String moduleName, String url) {
        if (type.equals(myType)) {
            return createUrl(moduleName, url);
        }
        return null;
    }

    protected abstract AbstractUrl createUrl(String moduleName, String url);

    public abstract AbstractUrl createUrlByElement(Object element);

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractUrl that = (AbstractUrl) o;

        return Objects.equals(moduleName, that.moduleName)
            && Objects.equals(myType, that.myType)
            && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(url);
        result = 29 * result + Objects.hashCode(moduleName);
        return 29 * result + Objects.hashCode(myType);
    }
}
