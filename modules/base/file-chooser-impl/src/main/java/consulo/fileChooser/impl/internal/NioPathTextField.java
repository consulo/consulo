// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.application.Application;
import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.UIAccess;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

class NioPathTextField implements PseudoComponent {
    private final TextBoxWithHistory myTextBox = TextBoxWithHistory.create();
    private final boolean myChooseFiles;
    private final boolean myChooseArchives;

    private BooleanSupplier myShowHiddenSupplier = () -> false;

    /**
     * Converts the text of this field into a path, see {@link UniversalFileChooserContributor#parsePresentablePath}.
     */
    private Function<String, @Nullable Path> myPathParser = text -> {
        try {
            return Path.of(text);
        }
        catch (RuntimeException e) {
            return null;
        }
    };

    private String myLastText = "";

    NioPathTextField(boolean chooseFiles, boolean chooseArchives) {
        myChooseFiles = chooseFiles;
        myChooseArchives = chooseArchives;

        myTextBox.addValueListener(event -> {
            String text = event.getValue();
            String previous = myLastText;
            myLastText = text == null ? "" : text;
            if (myLastText.length() == previous.length() + 1 && myLastText.startsWith(previous)) {
                String inserted = myLastText.substring(previous.length());
                if (inserted.equals("/") || inserted.equals("\\")) {
                    updateCompletion();
                }
            }
        });
    }

    @Override
    public Component getComponent() {
        return myTextBox;
    }

    TextBoxWithHistory getTextBox() {
        return myTextBox;
    }

    void setShowHiddenSupplier(BooleanSupplier showHiddenSupplier) {
        myShowHiddenSupplier = showHiddenSupplier;
    }

    void setPathParser(Function<String, @Nullable Path> pathParser) {
        myPathParser = pathParser;
    }

    private void updateCompletion() {
        String currentText = myTextBox.getValueOrError();
        Path directory = myPathParser.apply(currentText);
        if (directory == null) {
            return;
        }

        boolean showHidden = myShowHiddenSupplier.getAsBoolean();
        UIAccess uiAccess = UIAccess.current();
        Application.get().executeOnPooledThread(() -> {
            List<Path> children = NioFileChooserUtil.safeGetChildren(directory, showHidden, myChooseFiles, myChooseArchives);
            if (children.isEmpty()) {
                return;
            }
            List<String> history = new ArrayList<>();
            for (Path child : children) {
                history.add(child.toString());
            }
            uiAccess.give(() -> {
                if (currentText.equals(myTextBox.getValueOrError())) {
                    myTextBox.setHistory(history);
                }
            });
        });
    }
}
