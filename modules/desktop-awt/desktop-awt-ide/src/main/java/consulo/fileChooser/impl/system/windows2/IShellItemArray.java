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
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.Guid.REFIID;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.IUnknown;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import consulo.fileChooser.impl.system.windows2.ShTypes.GETPROPERTYSTOREFLAGS;
import consulo.fileChooser.impl.system.windows2.ShTypes.PROPERTYKEY;

public interface IShellItemArray extends IUnknown {

    IID IID_ISHELLITEMARRAY = new IID("{b63ea76d-1f85-456f-a19c-48159efa858b}");

    HRESULT BindToHandler(Pointer pbc, GUID.ByReference bhid, REFIID riid, PointerByReference ppvOut); // IBindCtx

    HRESULT GetPropertyStore(GETPROPERTYSTOREFLAGS flags, REFIID riid, PointerByReference ppv);

    HRESULT GetPropertyDescriptionList(PROPERTYKEY keyType, REFIID riid, PointerByReference ppv);

    HRESULT GetAttributes(int AttribFlags, int sfgaoMask, IntByReference psfgaoAttribs); // SIATTRIBFLAGS, SFGAOF,
                                                                                         // SFGAOF

    HRESULT GetCount(IntByReference pdwNumItems);

    HRESULT GetItemAt(int dwIndex, PointerByReference ppsi); // IShellItem
}
