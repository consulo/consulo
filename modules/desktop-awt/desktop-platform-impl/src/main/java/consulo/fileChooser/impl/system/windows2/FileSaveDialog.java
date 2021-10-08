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

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;

public class FileSaveDialog extends FileDialog implements IFileSaveDialog {

    public FileSaveDialog() {
    }

    public FileSaveDialog(Pointer pvInstance) {
        super(pvInstance);
    }

    // VTBL Id indexing starts at 27 after IFileDialog's 26

    public HRESULT SetSaveAsItem(Pointer psi) {
        return (HRESULT) this._invokeNativeObject(27, new Object[] { this.getPointer(), psi }, HRESULT.class);
    }

    public HRESULT SetProperties(Pointer pStore) {
        return (HRESULT) this._invokeNativeObject(28, new Object[] { this.getPointer(), pStore }, HRESULT.class);
    }

    public HRESULT SetCollectedProperties(Pointer pList, boolean fAppendDefault) {
        return (HRESULT) this._invokeNativeObject(29, new Object[] { this.getPointer(), pList, fAppendDefault },
                HRESULT.class);
    }

    public HRESULT GetProperties(PointerByReference ppStore) {
        return (HRESULT) this._invokeNativeObject(30, new Object[] { this.getPointer(), ppStore }, HRESULT.class);
    }

    public HRESULT ApplyProperties(Pointer psi, Pointer pStore, HWND hwnd, Pointer pSink) {
        return (HRESULT) this._invokeNativeObject(31, new Object[] { this.getPointer(), psi, pStore, hwnd, pSink },
                HRESULT.class);
    }
}
