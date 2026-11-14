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
package consulo.desktop.awt.ui.impl.clipboard;

import consulo.logging.Logger;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps between the payload of {@link consulo.ui.clipboard.Clipboard} and the flavor world of this
 * frontend, in both directions.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public final class DesktopAWTDataTransfers {
    private static final Logger LOG = Logger.getInstance(DesktopAWTDataTransfers.class);

    private static final Map<DataTransferType<?>, DataFlavor> FLAVORS = createFlavors();
    private static final DataFlavor LOCAL_FLAVOR = createLocalFlavor();

    private static DataFlavor createLocalFlavor() {
        try {
            return new DataFlavor(
                DataFlavor.javaJVMLocalObjectMimeType + ";class=" + DataTransfer.class.getName(),
                null,
                DataTransfer.class.getClassLoader()
            );
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<DataTransferType<?>, DataFlavor> createFlavors() {
        Map<DataTransferType<?>, DataFlavor> flavors = new LinkedHashMap<>();
        flavors.put(DataTransferType.TEXT, DataFlavor.stringFlavor);
        flavors.put(DataTransferType.FILE_LIST, DataFlavor.javaFileListFlavor);
        try {
            flavors.put(DataTransferType.HTML, new DataFlavor("text/html;class=java.lang.String"));
            flavors.put(DataTransferType.RTF, new DataFlavor("text/rtf;class=java.lang.String"));
        }
        catch (ClassNotFoundException e) {
            LOG.warn(e);
        }
        return flavors;
    }

    public static Map<DataTransferType<?>, DataFlavor> flavors() {
        return FLAVORS;
    }

    public static Transferable toTransferable(DataTransfer transfer) {
        return new DataTransferTransferable(transfer);
    }

    public static DataTransfer fromTransferable(Transferable transferable) {
        if (transferable.isDataFlavorSupported(LOCAL_FLAVOR)) {
            try {
                if (transferable.getTransferData(LOCAL_FLAVOR) instanceof DataTransfer transfer) {
                    return transfer;
                }
            }
            catch (Exception e) {
                LOG.warn(e);
            }
        }

        DataTransfer.Builder builder = DataTransfer.builder();
        readInto(builder, transferable, DataTransferType.TEXT, String.class);
        readInto(builder, transferable, DataTransferType.HTML, String.class);
        readInto(builder, transferable, DataTransferType.RTF, String.class);

        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            try {
                Object files = transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (files instanceof List<?> list) {
                    List<File> copy = new ArrayList<>();
                    for (Object file : list) {
                        if (file instanceof File value) {
                            copy.add(value);
                        }
                    }
                    builder.put(DataTransferType.FILE_LIST, copy);
                }
            }
            catch (Exception e) {
                LOG.warn(e);
            }
        }
        if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            byte[] png = readImage(transferable);
            if (png != null) {
                builder.put(DataTransferType.IMAGE, png);
            }
        }
        return builder.build();
    }

    /**
     * The image of the clipboard as png - a screenshot, a copied picture.
     */
    private static byte @Nullable [] readImage(Transferable transferable) {
        try {
            if (!(transferable.getTransferData(DataFlavor.imageFlavor) instanceof java.awt.Image image)) {
                return null;
            }
            BufferedImage buffered;
            if (image instanceof BufferedImage value) {
                buffered = value;
            }
            else {
                int width = image.getWidth(null);
                int height = image.getHeight(null);
                if (width <= 0 || height <= 0) {
                    return null;
                }
                buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = buffered.createGraphics();
                try {
                    graphics.drawImage(image, 0, 0, null);
                }
                finally {
                    graphics.dispose();
                }
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            return ImageIO.write(buffered, "png", png) ? png.toByteArray() : null;
        }
        catch (Exception e) {
            LOG.warn(e);
            return null;
        }
    }

    private static <T> void readInto(DataTransfer.Builder builder, Transferable transferable, DataTransferType<T> type, Class<T> valueClass) {
        DataFlavor flavor = FLAVORS.get(type);
        if (flavor == null || !transferable.isDataFlavorSupported(flavor)) {
            return;
        }

        try {
            Object data = transferable.getTransferData(flavor);
            if (valueClass.isInstance(data)) {
                builder.put(type, valueClass.cast(data));
            }
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }

    private DesktopAWTDataTransfers() {
    }

    private static class DataTransferTransferable implements Transferable {
        private final DataTransfer myTransfer;

        DataTransferTransferable(DataTransfer transfer) {
            myTransfer = transfer;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            List<DataFlavor> flavors = new ArrayList<>();
            if (hasLocalTypes()) {
                flavors.add(LOCAL_FLAVOR);
            }
            for (DataTransferType<?> type : myTransfer.getTypes()) {
                DataFlavor flavor = FLAVORS.get(type);
                if (flavor != null) {
                    flavors.add(flavor);
                }
            }
            return flavors.toArray(new DataFlavor[0]);
        }

        private boolean hasLocalTypes() {
            for (DataTransferType<?> type : myTransfer.getTypes()) {
                if (!type.isNative()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return Arrays.asList(getTransferDataFlavors()).contains(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (LOCAL_FLAVOR.equals(flavor) && hasLocalTypes()) {
                return myTransfer;
            }
            for (DataTransferType<?> type : myTransfer.getTypes()) {
                if (flavor.equals(FLAVORS.get(type))) {
                    Object value = myTransfer.get(type);
                    if (value != null) {
                        return value;
                    }
                }
            }
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
