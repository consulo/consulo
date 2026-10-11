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
package consulo.desktop.qt.ui.impl;

import io.qt.core.QModelIndex;
import io.qt.core.QObject;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.widgets.QApplication;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOptionViewItem;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public class DesktopQtCheckBoxTreeItemDelegate extends DesktopQtTextItemDelegate {
    private boolean myPaintingContent;

    public DesktopQtCheckBoxTreeItemDelegate(QObject parent) {
        super(parent);
    }

    public QRect checkBoxRect(QWidget view, QRect itemRect, QModelIndex index) {
        QStyleOptionViewItem option = new QStyleOptionViewItem();
        option.initFrom(view);
        option.setWidget(view);
        option.setRect(itemRect);
        initStyleOption(option, index);

        if (!option.features().testFlag(QStyleOptionViewItem.ViewItemFeature.HasCheckIndicator)) {
            return new QRect();
        }
        return view.style().subElementRect(QStyle.SubElement.SE_ItemViewItemCheckIndicator, option, view);
    }

    @Override
    protected void initStyleOption(QStyleOptionViewItem option, QModelIndex index) {
        super.initStyleOption(option, index);

        if (myPaintingContent) {
            QStyleOptionViewItem.ViewItemFeatures features = option.features();
            features.setFlag(QStyleOptionViewItem.ViewItemFeature.HasCheckIndicator, false);
            option.setFeatures(features);
        }
    }

    @Override
    public void paint(@Nullable QPainter painter, QStyleOptionViewItem option, QModelIndex index) {
        if (painter == null || !isCheckDisabled(index)) {
            super.paint(painter, option, index);
            return;
        }

        QStyleOptionViewItem full = new QStyleOptionViewItem(option);
        initStyleOption(full, index);

        QWidget widget = full.widget();
        QStyle style = widget != null ? widget.style() : QApplication.style();

        QRect checkRect = style.subElementRect(QStyle.SubElement.SE_ItemViewItemCheckIndicator, full, widget);
        int shift = contentLeft(style, full, widget);

        QStyleOptionViewItem stripped = new QStyleOptionViewItem(full);
        QStyleOptionViewItem.ViewItemFeatures features = stripped.features();
        features.setFlag(QStyleOptionViewItem.ViewItemFeature.HasCheckIndicator, false);
        stripped.setFeatures(features);
        shift -= contentLeft(style, stripped, widget);

        style.drawPrimitive(QStyle.PrimitiveElement.PE_PanelItemViewItem, full, painter, widget);

        QStyleOptionViewItem check = new QStyleOptionViewItem(full);
        check.setRect(checkRect);
        QStyle.State state = check.state();
        state.setFlag(QStyle.StateFlag.State_Enabled, false);
        state.setFlag(QStyle.StateFlag.State_HasFocus, false);
        state.setFlag(QStyle.StateFlag.State_On, full.checkState() == Qt.CheckState.Checked);
        state.setFlag(QStyle.StateFlag.State_Off, full.checkState() == Qt.CheckState.Unchecked);
        state.setFlag(QStyle.StateFlag.State_NoChange, full.checkState() == Qt.CheckState.PartiallyChecked);
        check.setState(state);
        QPalette palette = new QPalette(check.palette());
        palette.setCurrentColorGroup(QPalette.ColorGroup.Disabled);
        check.setPalette(palette);
        style.drawPrimitive(QStyle.PrimitiveElement.PE_IndicatorItemViewItemCheck, check, painter, widget);

        QStyleOptionViewItem content = new QStyleOptionViewItem(option);
        QRect rect = option.rect();
        content.setRect(new QRect(rect.x() + shift, rect.y(), Math.max(0, rect.width() - shift), rect.height()));

        myPaintingContent = true;
        try {
            super.paint(painter, content, index);
        }
        finally {
            myPaintingContent = false;
        }
    }

    private static int contentLeft(QStyle style, QStyleOptionViewItem option, @Nullable QWidget widget) {
        QStyle.SubElement element = option.features().testFlag(QStyleOptionViewItem.ViewItemFeature.HasDecoration)
            ? QStyle.SubElement.SE_ItemViewItemDecoration
            : QStyle.SubElement.SE_ItemViewItemText;
        return style.subElementRect(element, option, widget).left();
    }

    private static boolean isCheckDisabled(QModelIndex index) {
        if (!index.isValid() || index.data(Qt.ItemDataRole.CheckStateRole) == null) {
            return false;
        }
        return !index.flags().testFlag(Qt.ItemFlag.ItemIsUserCheckable);
    }
}
