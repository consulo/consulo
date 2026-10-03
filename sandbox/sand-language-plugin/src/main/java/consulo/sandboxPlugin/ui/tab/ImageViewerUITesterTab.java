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
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Size2D;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.image.Image;
import consulo.ui.image.viewer.ImageViewer;
import consulo.ui.image.viewer.ImageViewerChessboard;
import consulo.ui.image.viewer.ImageViewerGrid;
import consulo.ui.image.viewer.ImageViewerZoom;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.StandardColors;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "imageViewer", order = "after animatedImages")
public class ImageViewerUITesterTab implements UITesterTab {
    private static final List<String> SAMPLES = List.of(
        "pixel.png",
        "photo.jpg",
        "logo.svg",
        "anim.gif",
        "icon.bmp",
        "favicon.ico",
        "photo.webp",
        "scan.tiff",
        "misnamed.png",
        "broken.png"
    );

    private static final ImageViewerChessboard CHESSBOARD =
        new ImageViewerChessboard(10, StandardColors.WHITE.getStaticValue(), StandardColors.LIGHT_GRAY.getStaticValue());
    private static final ImageViewerGrid GRID = new ImageViewerGrid(16, 3, StandardColors.BLACK.getStaticValue());

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Image Viewer");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        ImageViewer viewer = ImageViewer.create();
        viewer.setChessboard(CHESSBOARD);
        viewer.setGrid(GRID);
        viewer.getZoom().setSmartZoom(new Size2D(128, 128));
        ImageViewerZoom zoom = viewer.getZoom();

        Label imageLabel = Label.create(LocalizeValue.empty());
        Label zoomLabel = Label.create(zoomText(zoom.getZoomFactor()));
        Label pointerLabel = Label.create(LocalizeValue.of("Pointer: -"));

        viewer.addZoomListener(event -> zoomLabel.setText(zoomText(event.getZoomFactor())));
        viewer.addPointerListener(event -> pointerLabel.setText(LocalizeValue.of(event.isOverImage()
            ? String.format(Locale.ROOT, "Pointer: %.3f, %.3f", event.getX(), event.getY())
            : "Pointer: -")));

        ComboBox<String> sampleBox = ComboBox.create(SAMPLES);
        sampleBox.setValue(SAMPLES.getFirst());
        viewer.setImage(load(SAMPLES.getFirst(), imageLabel));
        sampleBox.addValueListener(event -> {
            String sample = event.getValue();
            if (sample != null) {
                zoom.setZoomLevelChanged(false);
                viewer.setImage(load(sample, imageLabel));
            }
        });

        CheckBox gridBox = CheckBox.create(LocalizeValue.localizeTODO("Grid"), true);
        gridBox.addValueListener(event -> viewer.setGrid(event.getValue() ? GRID : null));

        CheckBox chessboardBox = CheckBox.create(LocalizeValue.localizeTODO("Chessboard"), true);
        chessboardBox.addValueListener(event -> viewer.setChessboard(event.getValue() ? CHESSBOARD : null));

        CheckBox wheelBox = CheckBox.create(LocalizeValue.localizeTODO("Wheel Zoom"), true);
        wheelBox.addValueListener(event -> viewer.setWheelZoomEnabled(event.getValue()));

        HorizontalLayout zoomRow = HorizontalLayout.create(Space.X_SMALL);
        zoomRow.add(sampleBox);
        zoomRow.add(Button.create(LocalizeValue.localizeTODO("Zoom In"), event -> zoom.zoomIn()));
        zoomRow.add(Button.create(LocalizeValue.localizeTODO("Zoom Out"), event -> zoom.zoomOut()));
        zoomRow.add(Button.create(LocalizeValue.localizeTODO("Actual"), event -> zoom.setZoomFactor(1)));
        zoomRow.add(Button.create(LocalizeValue.localizeTODO("Fit"), event -> zoom.zoomToFit()));

        HorizontalLayout optionsRow = HorizontalLayout.create(Space.MEDIUM);
        optionsRow.add(Button.create(LocalizeValue.localizeTODO("Clear"), event -> {
            sampleBox.setValue(null);
            imageLabel.setText(LocalizeValue.of("Image: -"));
            viewer.setImage(null);
        }));
        optionsRow.add(gridBox);
        optionsRow.add(chessboardBox);
        optionsRow.add(wheelBox);

        VerticalLayout toolbar = VerticalLayout.create(Space.X_SMALL);
        toolbar.add(zoomRow);
        toolbar.add(optionsRow);

        HorizontalLayout status = HorizontalLayout.create(Space.LARGE);
        status.add(imageLabel);
        status.add(zoomLabel);
        status.add(pointerLabel);

        return DockLayout.create().top(toolbar).center(viewer).bottom(status);
    }

    @RequiredUIAccess
    private static @Nullable Image load(String sample, Label imageLabel) {
        Image.ImageType type = Image.ImageType.fromFileName(sample);
        try (InputStream stream = ImageViewerUITesterTab.class.getResourceAsStream("/uiTester/images/" + sample)) {
            if (stream == null) {
                imageLabel.setText(LocalizeValue.of("Image: no resource " + sample));
                return null;
            }

            Image image = Image.fromBytes(type, stream.readAllBytes());
            imageLabel.setText(LocalizeValue.of("Image: " + sample + " " + image.getWidth() + "x" + image.getHeight()));
            return image;
        }
        catch (IOException e) {
            imageLabel.setText(LocalizeValue.of("Image: " + sample + " not readable - " + e.getMessage()));
            return null;
        }
    }

    private static LocalizeValue zoomText(double zoomFactor) {
        return LocalizeValue.of(String.format(Locale.ROOT, "Zoom: %.0f%%", zoomFactor * 100));
    }
}
