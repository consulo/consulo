/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.awt.ui.impl.messageList;

import consulo.ui.ex.awt.JBHtmlEditorKit;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import org.jspecify.annotations.Nullable;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
final class MessageBodyEditorKit extends JBHtmlEditorKit {
    private final ViewFactory myViewFactory = new JBHtmlFactory() {
        @Override
        public View create(Element elem) {
            if ("img".equals(elem.getName())) {
                return new ResizingHtmlImageView(elem);
            }
            return super.create(elem);
        }
    };

    private @Nullable AsyncHtmlImageLoader myImageLoader;

    MessageBodyEditorKit() {
        super(true);
    }

    void setImageLoader(@Nullable AsyncHtmlImageLoader imageLoader) {
        myImageLoader = imageLoader;
    }

    @Override
    public Document createDefaultDocument() {
        Document document = super.createDefaultDocument();
        document.putProperty(ResizingHtmlImageView.LOADER_PROPERTY, myImageLoader);
        return document;
    }

    @Override
    public ViewFactory getViewFactory() {
        return myViewFactory;
    }
}
