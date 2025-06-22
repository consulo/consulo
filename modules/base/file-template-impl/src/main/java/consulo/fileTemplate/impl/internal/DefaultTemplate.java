/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.fileTemplate.impl.internal;

import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;

/**
 * @author Eugene Zhuravlev
 * @since 2011-03-28
 */
public class DefaultTemplate {
    private static final Logger LOG = Logger.getInstance(DefaultTemplate.class);

    private final String myName;
    private final String myExtension;
    private final FileTemplateStreamProvider myTemplateURL;
    @Nullable
    private final FileTemplateStreamProvider myDescriptionURL;
    private final String myText;
    private final String myDescriptionText;

    public DefaultTemplate(@Nonnull String name,
                           @Nonnull String extension,
                           @Nonnull FileTemplateStreamProvider templateURL,
                           @Nullable FileTemplateStreamProvider descriptionURL) {
        myName = name;
        myExtension = extension;
        myTemplateURL = templateURL;
        myDescriptionURL = descriptionURL;
        myText = loadText(templateURL);
        myDescriptionText = descriptionURL != null ? loadText(descriptionURL) : "";
    }

    private static String loadText(FileTemplateStreamProvider url) {
        String text = "";
        try {
            text = url.loadText();
        }
        catch (IOException e) {
            LOG.error(e);
        }
        return text;
    }

    public String getName() {
        return myName;
    }

    public String getQualifiedName() {
        return FileTemplateBase.getQualifiedName(getName(), getExtension());
    }

    public String getExtension() {
        return myExtension;
    }

    public FileTemplateStreamProvider getTemplateURL() {
        return myTemplateURL;
    }

    @Nullable
    public FileTemplateStreamProvider getDescriptionURL() {
        return myDescriptionURL;
    }

    @Nonnull
    public String getText() {
        return myText;
    }

    @Nonnull
    public String getDescriptionText() {
        return myDescriptionText;
    }
}
