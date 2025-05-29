// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inlay;

import consulo.codeEditor.Editor;

public interface RootInlayPresentation<Content> extends InlayPresentation {
    /**
     * Method is called on old presentation to apply updates to its content.
     * <p>
     * This action should be FAST, it executes inside write action!
     * This method is called only if keys of presentations are the same.
     *
     * @param newPresentationContent content of the root of the NEW presentation
     * @param editor                 the editor instance
     * @param factory                the factory to create sub-presentations
     * @return true if something has changed
     */
    boolean update(Content newPresentationContent, Editor editor, InlayPresentationFactory factory);

    /**
     * @return the content held by this presentation
     */
    Content getContent();

    /**
     * @return the unique key identifying this presentation's content type
     */
    ContentKey<Content> getKey();
}
