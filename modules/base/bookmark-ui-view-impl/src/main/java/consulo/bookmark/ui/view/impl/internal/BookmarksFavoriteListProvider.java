/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.bookmark.ui.view.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.bookmark.Bookmark;
import consulo.bookmark.BookmarkManager;
import consulo.bookmark.event.BookmarksListener;
import consulo.bookmark.icon.BookmarkIconGroup;
import consulo.bookmark.internal.BookmarkIcon;
import consulo.bookmark.localize.BookmarkLocalize;
import consulo.bookmark.ui.view.AbstractFavoritesListProvider;
import consulo.bookmark.ui.view.FavoritesManager;
import consulo.bookmark.ui.view.internal.BookmarkItem;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.awt.CommonActionsPanel;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Vassiliy.Kudryashov
 */
@ExtensionImpl
public class BookmarksFavoriteListProvider extends AbstractFavoritesListProvider<Bookmark> implements BookmarksListener {
    private final BookmarkManager myBookmarkManager;
    private final FavoritesManager myFavoritesManager;

    @Inject
    public BookmarksFavoriteListProvider(Project project, BookmarkManager bookmarkManager, FavoritesManager favoritesManager) {
        super(project, "Bookmarks");
        myBookmarkManager = bookmarkManager;
        myFavoritesManager = favoritesManager;
        project.getMessageBus().connect(project).subscribe(BookmarksListener.class, this);
        updateChildren();
    }

    @Override
    public void bookmarkAdded(@Nonnull Bookmark b) {
        updateChildren();
    }

    @Override
    public void bookmarkRemoved(@Nonnull Bookmark b) {
        updateChildren();
    }

    @Override
    public void bookmarkChanged(@Nonnull Bookmark b) {
        updateChildren();
    }

    @Override
    public String getListName(Project project) {
        return "Bookmarks";
    }

    private void updateChildren() {
        if (myProject.isDisposed()) {
            return;
        }
        myChildren.clear();
        List<Bookmark> bookmarks = myBookmarkManager.getValidBookmarks();
        for (final Bookmark bookmark : bookmarks) {
            AbstractTreeNode<Bookmark> child = new AbstractTreeNode<>(myProject, bookmark) {
                @Nonnull
                @Override
                @RequiredReadAction
                public Collection<? extends AbstractTreeNode> getChildren() {
                    return Collections.emptyList();
                }

                @Override
                public boolean canNavigate() {
                    return bookmark.canNavigate();
                }

                @Override
                public boolean canNavigateToSource() {
                    return bookmark.canNavigateToSource();
                }

                @Override
                public void navigate(boolean requestFocus) {
                    bookmark.navigate(requestFocus);
                }

                @Override
                protected void update(PresentationData presentation) {
                    presentation.setPresentableText(bookmark.toString());
                    presentation.setIcon(bookmark.getIcon(false));
                }
            };
            child.setParent(myNode);
            myChildren.add(child);
        }
        myFavoritesManager.fireListeners(getListName(myProject));
    }

    @Nonnull
    @Override
    public LocalizeValue getCustomName(@Nonnull CommonActionsPanel.Buttons type) {
        return switch (type) {
            case EDIT -> BookmarkLocalize.actionBookmarkEditDescription();
            case REMOVE -> BookmarkLocalize.actionBookmarkDelete();
            default -> LocalizeValue.empty();
        };
    }

    @Override
    public boolean willHandle(@Nonnull CommonActionsPanel.Buttons type, Project project, @Nonnull Set<Object> selectedObjects) {
        return switch (type) {
            case EDIT -> selectedObjects.size() == 1
                && selectedObjects.iterator().next() instanceof AbstractTreeNode toEdit
                && toEdit.getValue() instanceof Bookmark;
            case REMOVE -> {
                for (Object toRemove : selectedObjects) {
                    if (!(toRemove instanceof AbstractTreeNode toRemove1 && toRemove1.getValue() instanceof Bookmark)) {
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public void handle(
        @Nonnull CommonActionsPanel.Buttons type,
        Project project,
        @Nonnull Set<Object> selectedObjects,
        JComponent component
    ) {
        switch (type) {
            case EDIT -> {
                if (selectedObjects.size() == 1
                    && selectedObjects.iterator().next() instanceof AbstractTreeNode toEdit
                    && toEdit.getValue() instanceof Bookmark bookmark) {
                    BookmarkManager.getInstance(project).editDescription(bookmark);
                }
            }
            case REMOVE -> {
                for (Object toRemove : selectedObjects) {
                    Bookmark bookmark = (Bookmark) ((AbstractTreeNode) toRemove).getValue();
                    BookmarkManager.getInstance(project).removeBookmark(bookmark);
                }
            }
        }
    }

    @Override
    public int getWeight() {
        return BOOKMARKS_WEIGHT;
    }

    @Override
    @RequiredReadAction
    public void customizeRenderer(
        ColoredTreeCellRenderer renderer,
        JTree tree,
        @Nonnull Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    ) {
        renderer.clear();
        if (value instanceof Bookmark bookmark) {
            renderer.setIcon(BookmarkIcon.getDefaultIcon(false));
            BookmarkItem.setupRenderer(renderer, myProject, bookmark, selected);
            if (renderer.getIcon() != null) {
                renderer.setIcon(ImageEffects.appendRight(bookmark.getIcon(false), renderer.getIcon()));
            }
            else {
                renderer.setIcon(bookmark.getIcon(false));
            }
        }
        else {
            renderer.setIcon(BookmarkIconGroup.bookmarkslist());
            renderer.append(getListName(myProject));
        }
    }
}
