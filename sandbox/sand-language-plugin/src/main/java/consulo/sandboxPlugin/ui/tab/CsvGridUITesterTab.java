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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorFontType;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.grid.editor.CsvDocumentDataHookUp;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextArea;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.ex.grid.action.SetFirstRowIsHeaderAction;
import consulo.ui.ex.grid.action.TableResultColumnHeaderPopupGroup;
import consulo.ui.ex.grid.action.TableResultPopupGroup;
import consulo.ui.ex.grid.csv.CsvFormat;
import consulo.ui.ex.grid.csv.CsvFormats;
import consulo.ui.ex.grid.csv.CsvFormatter;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridRequestPlace;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.ThrowableInfo;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The data grid over CSV text: an in-memory document - with comment lines, quoted values holding a separator, a line break and quotes,
 * an empty last value and a short row - shown as text beside a grid over {@link CsvDocumentDataHookUp}. Every change of the data made in
 * the grid - a value, a row, a column - is written into the document as a minimal change, and the text follows the document.
 * <p/>
 * The grid is set up like a CSV table: every value is text, the delete key clears the selected cells, a row shows up to three lines. The
 * format starts without a header row; "First row is header" - the check box, or the item of the context menus - takes the names of the
 * columns from the first record without changing the text. Renaming a column while there is no header row writes one.
 *
 * @since 2026-10-04
 */
@ExtensionImpl(id = "csvGrid", order = "after grid")
public class CsvGridUITesterTab implements UITesterTab {
    // the quote before the comma of the "Cherry" line is escaped: three quotes in a row would end the text block
    private static final String SAMPLE_CSV = """
        # export
        id,name,qty,note
        1,Apple,10,"fresh, red"
        2,Banana,25,"line one
        line two"
        # mid
        3,"Cherry ""sweet""\",7,
        4,Date
        """;

    private static final int ROW_LINES = 3;
    /**
     * The share of the text, in percent of the width.
     */
    private static final int TEXT_PROPORTION = 35;

    private final EditorFactory myEditorFactory;
    private final EditorColorsManager myEditorColorsManager;

    @Inject
    public CsvGridUITesterTab(EditorFactory editorFactory, EditorColorsManager editorColorsManager) {
        myEditorFactory = editorFactory;
        myEditorColorsManager = editorColorsManager;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > Grid (CSV)");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        UIAccess uiAccess = UIAccess.current();
        Document document = myEditorFactory.createDocument(SAMPLE_CSV);

        // the data source parses on a background thread and answers on this thread. It goes before the grid, so it is disposed after it
        CsvDocumentDataHookUp hookUp = new CsvDocumentDataHookUp(null, CsvFormats.CSV_FORMAT.get(), document, null, uiAccess);
        Disposer.register(uiDisposable, hookUp);

        DataGrid grid = GridUtil.createDataGrid(
            hookUp,
            TableResultPopupGroup.ID,
            TableResultColumnHeaderPopupGroup.ID,
            (dataGrid, appearance) -> {
                GridUtil.configureCsvTable(dataGrid, appearance);
                // the delete key clears the selected cells; Delete Rows stays in the context menu
                dataGrid.putUserData(GridUtil.DELETE_CLEARS_CELLS, true);
                appearance.setRowLines(ROW_LINES);
            }
        );
        Disposer.register(uiDisposable, grid);

        // the item of the context menus asks the grid for its handler
        grid.putUserData(SetFirstRowIsHeaderAction.Handler.KEY, new SetFirstRowIsHeaderAction.Handler() {
            @Override
            public boolean firstRowIsHeader() {
                return hasHeader(hookUp);
            }

            @Override
            @RequiredUIAccess
            public void setFirstRowIsHeader(boolean selected) {
                setHeader(hookUp, grid, selected);
            }
        });

        CheckBox headerBox = CheckBox.create(LocalizeValue.localizeTODO("First row is header"));
        headerBox.setValue(hasHeader(hookUp), false);
        headerBox.addValueListener(event -> setHeader(hookUp, grid, Boolean.TRUE.equals(event.getValue())));

        TextArea textArea = TextArea.create(document.getImmutableCharSequence().toString());
        textArea.setEditable(false);
        // the font of the editor, which the grid shows its values in as well
        textArea.setFont(myEditorColorsManager.getGlobalScheme().getFont(EditorFontType.PLAIN));
        document.addDocumentListener(new TextFollower(document, textArea, uiAccess, uiDisposable), uiDisposable);

        Label statusLabel = Label.create(LocalizeValue.empty());
        // the data source reports its requests on the UI thread
        hookUp.addRequestListener(new GridDataHookUp.RequestListener<>() {
            @Override
            @RequiredUIAccess
            public void error(GridRequestSource source, ThrowableInfo errorInfo) {
                LocalizeValue message = LocalizeValue.of(errorInfo.getMessage());
                statusLabel.setText(LocalizeValue.join(LocalizeValue.localizeTODO("Request failed: "), message));
            }

            @Override
            public void updateCountReceived(GridRequestSource source, int updateCount) {
            }

            @Override
            @RequiredUIAccess
            public void requestFinished(GridRequestSource source, boolean success) {
                if (success) {
                    statusLabel.setText(LocalizeValue.empty());
                }
                // renaming a column while there is no header row writes one, and turns the header on
                headerBox.setValue(hasHeader(hookUp), false);
            }
        }, uiDisposable);

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(textArea);
        split.setSecondComponent(grid);
        split.setProportion(TEXT_PROPORTION);

        return DockLayout.create().top(headerBox).center(split).bottom(statusLabel);
    }

    private static boolean hasHeader(CsvDocumentDataHookUp hookUp) {
        return hookUp.getFormat().headerRecord != null;
    }

    /**
     * Parses the text again with or without a header row; the text stays as it is.
     */
    @RequiredUIAccess
    private static void setHeader(CsvDocumentDataHookUp hookUp, DataGrid grid, boolean header) {
        CsvFormat format = hookUp.getFormat();
        if ((format.headerRecord != null) == header) {
            return;
        }
        if (!grid.stopEditing()) {
            grid.cancelEditing();
        }
        hookUp.setFormat(CsvFormatter.setFirstRowIsHeader(format, header), new GridRequestSource(new DataGridRequestPlace(grid)));
    }

    /**
     * Shows the text of the document once it changed. The changes of one command are shown at once, on the UI thread, after they are
     * all made.
     */
    private static final class TextFollower implements DocumentListener {
        private final Document myDocument;
        private final TextArea myTextArea;
        private final UIAccess myUIAccess;
        private final Disposable myUIDisposable;
        private final AtomicBoolean myUpdateScheduled = new AtomicBoolean();

        private TextFollower(Document document, TextArea textArea, UIAccess uiAccess, Disposable uiDisposable) {
            myDocument = document;
            myTextArea = textArea;
            myUIAccess = uiAccess;
            myUIDisposable = uiDisposable;
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            scheduleUpdate();
        }

        /**
         * The data source writes its changes in bulk mode.
         */
        @Override
        public void bulkUpdateFinished(Document document) {
            scheduleUpdate();
        }

        private void scheduleUpdate() {
            if (myUpdateScheduled.compareAndSet(false, true)) {
                myUIAccess.give(this::update);
            }
        }

        @RequiredUIAccess
        private void update() {
            myUpdateScheduled.set(false);
            // the tester may be closed meanwhile
            if (Disposer.isDisposed(myUIDisposable)) {
                return;
            }
            myTextArea.setValue(myDocument.getImmutableCharSequence().toString());
        }
    }
}
