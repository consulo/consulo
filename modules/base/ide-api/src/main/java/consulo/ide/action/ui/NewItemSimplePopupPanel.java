// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.action.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.IdeBundle;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.ValidableComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBPanel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.layout.WrappedLayout;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.function.Consumer;

public class NewItemSimplePopupPanel extends JBPanel implements Disposable {
    protected final TextBoxWithExtensions myTextField;

    private JBPopup myErrorPopup;
    protected RelativePoint myErrorShowPoint;

    protected Consumer<? super InputEvent> myApplyAction;

    @RequiredUIAccess
    public NewItemSimplePopupPanel() {
        super(new BorderLayout());

        myTextField = createTextField();

        WrappedLayout layout = WrappedLayout.create(myTextField);
        layout.addBorder(BorderPosition.TOP, BorderStyle.LINE);

        add(TargetAWT.to(layout), BorderLayout.NORTH);

        //myErrorShowPoint = new RelativePoint(myTextField, new Point(0, myTextField.getHeight()));
    }

    public void addValidator(@Nonnull ValidableComponent.Validator<String> validator) {
        myTextField.addValidator(validator);
    }

    public void setApplyAction(@Nonnull Consumer<? super InputEvent> applyAction) {
        myApplyAction = applyAction;
    }

    @Deprecated
    public void setError(String error) {
        ///myTextField.putClientProperty("JComponent.outline", error != null ? "error" : null);

        if (myErrorPopup != null && !myErrorPopup.isDisposed()) {
            Disposer.dispose(myErrorPopup);
        }
        if (error == null) {
            return;
        }
        //
        //ComponentPopupBuilder popupBuilder = ComponentValidator.createPopupBuilder(new ValidationInfo(error, myTextField), errorHint -> {
        //  Insets insets = myTextField.getInsets();
        //  Dimension hintSize = errorHint.getPreferredSize();
        //  Point point = new Point(0, insets.top - JBUIScale.scale(6) - hintSize.height);
        //  myErrorShowPoint = new RelativePoint(myTextField, point);
        //}).setCancelOnWindowDeactivation(false).setCancelOnClickOutside(true).addUserData("SIMPLE_WINDOW");

        //myErrorPopup = popupBuilder.createPopup();
        //myErrorPopup.show(myErrorShowPoint);
    }

    @Override
    public void dispose() {
        if (myErrorPopup != null && !myErrorPopup.isDisposed()) {
            Disposer.dispose(myErrorPopup);
        }
    }

    public TextBoxWithExtensions getTextField() {
        return myTextField;
    }

    @Nonnull
    protected TextBoxWithExtensions createTextField() {
        TextBoxWithExtensions res = TextBoxWithExtensions.create();

        res.setVisibleLength(30);

        res.addBorders(BorderStyle.EMPTY, null, 4);

        //Border errorBorder = new ErrorBorder(res.getBorder());
        //res.setBorder(JBUI.Borders.merge(border, errorBorder, false));
        //res.setBackground(JBUI.CurrentTheme.NewClassDialog.searchFieldBackground());

        //res.putClientProperty("StatusVisibleFunction", (BooleanFunction<JBTextField>)field -> field.getText().isEmpty());
        res.setPlaceholder(IdeBundle.message("action.create.new.class.name.field"));
        res.addKeyPressedListener(e -> {
            KeyboardInputDetails details = e.getInputDetails();
            KeyCode keyCode = details.getKeyCode();

            if (KeyCode.ENTER.equals(keyCode)) {
                if (!myTextField.validate()) {
                    return;
                }

                if (myApplyAction != null) {
                    myApplyAction.accept(null); // todo null ??
                }
            }
        });


        //res.addValueListener(event -> {
        //  setError(null);
        //});
        return res;
    }

    //private static class ErrorBorder implements Border {
    //  private final Border errorDelegateBorder;
    //
    //  private ErrorBorder(Border delegate) {
    //    errorDelegateBorder = delegate;
    //  }
    //
    //  @Override
    //  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    //    if (checkError(c)) {
    //      errorDelegateBorder.paintBorder(c, g, x, y, width, height);
    //    }
    //  }
    //
    //  @Override
    //  public Insets getBorderInsets(Component c) {
    //    return checkError(c) ? errorDelegateBorder.getBorderInsets(c) : JBUI.emptyInsets();
    //  }
    //
    //  @Override
    //  public boolean isBorderOpaque() {
    //    return false;
    //  }
    //
    //  private static boolean checkError(Component c) {
    //    Object outlineObj = ((JComponent)c).getClientProperty("JComponent.outline");
    //    if (outlineObj == null) return false;
    //
    //    DarculaUIUtil.Outline outline = outlineObj instanceof DarculaUIUtil.Outline ? (DarculaUIUtil.Outline)outlineObj : DarculaUIUtil.Outline.valueOf(outlineObj.toString());
    //    return outline == DarculaUIUtil.Outline.error || outline == DarculaUIUtil.Outline.warning;
    //  }
    //}
}
