/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ui.ex.awt;


import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.xml.XmlStringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.function.Consumer;

public class DetailsComponent {

    private final JPanel myComponent;

    private JComponent myContent;

    private final Banner myBannerLabel;

    private final JLabel myEmptyContentLabel;
    private final NonOpaquePanel myBanner;

    private String[] myBannerText;
    private boolean myDetailsEnabled;
    private boolean myPaintBorder;
    private String[] myPrefix;
    private String[] myText;

    private final Wrapper myContentGutter = new Wrapper();

    public DetailsComponent() {
        this(true, true);
    }

    public DetailsComponent(boolean detailsEnabled, boolean paintBorder) {
        myDetailsEnabled = detailsEnabled;
        myPaintBorder = paintBorder;
        myComponent = new JPanel(new BorderLayout());

        myComponent.setOpaque(false);
        myContentGutter.setOpaque(false);
        myContentGutter.setBorder(null);

        myBanner = new NonOpaquePanel(new BorderLayout());
        myBannerLabel = new Banner();

        if (myDetailsEnabled) {
            myBanner.add(myBannerLabel, BorderLayout.CENTER);
        }

        myEmptyContentLabel = new JLabel("", SwingConstants.CENTER);

        revalidateDetailsMode();
    }

    private void revalidateDetailsMode() {
        myComponent.removeAll();
        myComponent.add(myContentGutter, BorderLayout.CENTER);

        if (myDetailsEnabled) {
            myComponent.add(myBanner, BorderLayout.NORTH);
        }

        if (myContent != null) {
            myContentGutter.add(myContent, BorderLayout.CENTER);
            invalidateContentBorder();
        }

        myComponent.revalidate();
        myComponent.repaint();
    }

    public void setBannerActions(Action[] actions) {
        myBannerLabel.clearActions();
        for (Action each : actions) {
            myBannerLabel.addAction(each);
        }

        myComponent.revalidate();
        myComponent.repaint();
    }

    public void setFullContent(@Nonnull JComponent c, Consumer<JComponent> bannerLabelSetter) {
        myBanner.setVisible(false);

        myBanner.remove(myBannerLabel);

        bannerLabelSetter.accept(myBannerLabel);

        setContentImpl(c);
    }

    public void setContent(@Nullable JComponent c) {
        myBanner.setVisible(true);

        if (myDetailsEnabled && myBannerLabel.getParent() != myBanner) {
            myBanner.add(myBannerLabel, BorderLayout.CENTER);
        }

        setContentImpl(c);
    }

    private void setContentImpl(@Nullable JComponent c) {
        if (myContent != null) {
            myContentGutter.remove(myContent);
        }

        myContent = new MyWrapper(c);

        myContent.setOpaque(false);

        invalidateContentBorder();

        myContentGutter.add(myContent, BorderLayout.CENTER);

        updateBanner();

        myComponent.revalidate();
        myComponent.repaint();
    }

    private void invalidateContentBorder() {
        if (myDetailsEnabled && myPaintBorder) {
            myContent.setBorder(new EmptyBorder(0, 8, 0, 8));
        }
        else {
            myContent.setBorder(null);
        }
    }

    public void setProjectIconDescription(@Nullable String toolTipText) {
        myBannerLabel.setProjectIconDescription(toolTipText);
    }

    public void setPrefix(@Nullable String... prefix) {
        myPrefix = prefix;
        if (myText != null) {
            setText(myText);
        }
    }

    public void setText(@Nullable String... text) {
        myText = text;
        update();
    }

    public void update() {
        ArrayList<String> strings = new ArrayList<String>();
        if (myPrefix != null) {
            ContainerUtil.addAll(strings, myPrefix);
        }

        if (myText != null) {
            ContainerUtil.addAll(strings, myText);
        }

        myBannerText = ArrayUtil.toStringArray(strings);

        updateBanner();
    }

    private void updateBanner() {
        myBannerLabel.setText(NullableComponent.Check.isNull(myContent) || myBannerText == null ? ArrayUtil.EMPTY_STRING_ARRAY : myBannerText);

        myBannerLabel.revalidate();
        myBannerLabel.repaint();
    }

    public DetailsComponent setEmptyContentText(@Nullable final String emptyContentText) {
        final String s = XmlStringUtil.wrapInHtml("<center>" + (emptyContentText != null ? emptyContentText : "") + "</center>");
        myEmptyContentLabel.setText(s);
        return this;
    }

    public JComponent getComponent() {
        return myComponent;
    }

    public JComponent getContentGutter() {
        return myContentGutter;
    }

    public DetailsComponent setBannerMinHeight(final int height) {
        myBannerLabel.setMinHeight(height);
        return this;
    }

    public void disposeUIResources() {
        setContent(null);
    }

    public void updateBannerActions() {
        myBannerLabel.updateActions();
    }

    public void setDetailsModeEnabled(final boolean enabled) {
        if (myDetailsEnabled == enabled) {
            return;
        }

        myDetailsEnabled = enabled;

        revalidateDetailsMode();
    }

    private class MyWrapper extends Wrapper implements NullableComponent {
        public MyWrapper(final JComponent c) {
            super(c == null || NullableComponent.Check.isNull(c) ? myEmptyContentLabel : c);
        }

        @Override
        public boolean isNull() {
            return getTargetComponent() == myEmptyContentLabel;
        }
    }
}
