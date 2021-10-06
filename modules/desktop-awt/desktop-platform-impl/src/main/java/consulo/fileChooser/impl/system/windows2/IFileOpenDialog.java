/* Copyright (c) 2020 Daniel Widdis, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package consulo.fileChooser.impl.system.windows2;

import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;

public interface IFileOpenDialog extends IFileDialog {

    IID IID_IFILEOPENDIALOG = new IID("{d57c7288-d4ad-4768-be02-9d969532d960}");
    CLSID CLSID_FILEOPENDIALOG = new CLSID("{DC1C5A9C-E88A-4dde-A5A1-60F82A20AEF7}");

    HRESULT GetResults(PointerByReference ppenum); // IShellItemArray

    HRESULT GetSelectedItems(PointerByReference ppsai); // IShellItemArray
}
