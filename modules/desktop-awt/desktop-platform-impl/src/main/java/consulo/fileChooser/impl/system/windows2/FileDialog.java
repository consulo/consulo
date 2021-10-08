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
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import consulo.fileChooser.impl.system.windows2.ShTypes.COMDLG_FILTERSPEC;

public class FileDialog extends ModalWindow implements IFileDialog {

    public FileDialog() {
    }

    public FileDialog(Pointer pvInstance) {
        super(pvInstance);
    }

    // VTBL Id indexing starts at 4 after ModalWindow's 3

    public HRESULT SetFileTypes(int FileTypes, COMDLG_FILTERSPEC[] rgFilterSpec) {
        return (HRESULT) this._invokeNativeObject(4, new Object[] { this.getPointer(), FileTypes, rgFilterSpec },
                HRESULT.class);
    }

    public HRESULT SetFileTypeIndex(int iFileType) {
        return (HRESULT) this._invokeNativeObject(5, new Object[] { this.getPointer(), iFileType }, HRESULT.class);
    }

    public HRESULT GetFileTypeIndex(IntByReference piFileType) {
        return (HRESULT) this._invokeNativeObject(6, new Object[] { this.getPointer(), piFileType }, HRESULT.class);
    }

    public HRESULT Advise(Pointer pfde, IntByReference pdwCookie) {
        return (HRESULT) this._invokeNativeObject(7, new Object[] { this.getPointer(), pfde, pdwCookie },
                HRESULT.class);
    }

    public HRESULT Unadvise(int dwCookie) {
        return (HRESULT) this._invokeNativeObject(8, new Object[] { this.getPointer(), dwCookie }, HRESULT.class);
    }

    public HRESULT SetOptions(int fos) {
        return (HRESULT) this._invokeNativeObject(9, new Object[] { this.getPointer(), fos }, HRESULT.class);
    }

    public HRESULT GetOptions(IntByReference pfos) {
        return (HRESULT) this._invokeNativeObject(10, new Object[] { this.getPointer(), pfos }, HRESULT.class);
    }

    public HRESULT SetDefaultFolder(Pointer psi) {
        return (HRESULT) this._invokeNativeObject(11, new Object[] { this.getPointer(), psi }, HRESULT.class);
    }

    public HRESULT SetFolder(Pointer psi) {
        return (HRESULT) this._invokeNativeObject(12, new Object[] { this.getPointer(), psi }, HRESULT.class);
    }

    public HRESULT GetFolder(PointerByReference ppsi) {
        return (HRESULT) this._invokeNativeObject(13, new Object[] { this.getPointer(), ppsi }, HRESULT.class);
    }

    public HRESULT GetCurrentSelection(PointerByReference ppsi) {
        return (HRESULT) this._invokeNativeObject(14, new Object[] { this.getPointer(), ppsi }, HRESULT.class);
    }

    public HRESULT SetFileName(WString pszName) {
        return (HRESULT) this._invokeNativeObject(15, new Object[] { this.getPointer(), pszName }, HRESULT.class);
    }

    public HRESULT GetFileName(PointerByReference pszName) {
        return (HRESULT) this._invokeNativeObject(16, new Object[] { this.getPointer(), pszName }, HRESULT.class);
    }

    public HRESULT SetTitle(WString pszTitle) {
        return (HRESULT) this._invokeNativeObject(17, new Object[] { this.getPointer(), pszTitle }, HRESULT.class);
    }

    public HRESULT SetOkButtonLabel(WString pszText) {
        return (HRESULT) this._invokeNativeObject(18, new Object[] { this.getPointer(), pszText }, HRESULT.class);
    }

    public HRESULT SetFileNameLabel(WString pszLabel) {
        return (HRESULT) this._invokeNativeObject(19, new Object[] { this.getPointer(), pszLabel }, HRESULT.class);
    }

    public HRESULT GetResult(PointerByReference ppsi) {
        return (HRESULT) this._invokeNativeObject(20, new Object[] { this.getPointer(), ppsi }, HRESULT.class);
    }

    public HRESULT AddPlace(Pointer psi, int fdap) {
        return (HRESULT) this._invokeNativeObject(21, new Object[] { this.getPointer(), psi, fdap }, HRESULT.class);
    }

    public HRESULT SetDefaultExtension(WString pszDefaultExtension) {
        return (HRESULT) this._invokeNativeObject(22, new Object[] { this.getPointer(), pszDefaultExtension },
                HRESULT.class);
    }

    public HRESULT Close(HRESULT hr) {
        return (HRESULT) this._invokeNativeObject(23, new Object[] { this.getPointer(), hr }, HRESULT.class);
    }

    public HRESULT SetClientGuid(com.sun.jna.platform.win32.Guid.GUID.ByReference guid) {
        return (HRESULT) this._invokeNativeObject(24, new Object[] { this.getPointer(), guid }, HRESULT.class);
    }

    public HRESULT ClearClientData() {
        return (HRESULT) this._invokeNativeObject(25, new Object[] { this.getPointer() }, HRESULT.class);
    }

    public HRESULT SetFilter(Pointer pFilter) {
        return (HRESULT) this._invokeNativeObject(26, new Object[] { this.getPointer(), pFilter }, HRESULT.class);
    }
}
