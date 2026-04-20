//! Windows-only: convert VM option strings to the Active Code Page before
//! passing to `JNI_CreateJavaVM`, and inject `-Dsun.jnu.encoding.sys` when
//! the process ACP is UTF-8 so JNI text handling stays correct on CJK
//! installs.
//!
//! Ported from [XPlatLauncher java.rs:250-300].

#![cfg(target_os = "windows")]

use anyhow::{Context, Result, bail};
use log::debug;
use std::ffi::CString;

/// Convert `vm_options` (UTF-8) to CStrings encoded in the current ACP.
/// If the ACP is UTF-8, a `sun.jnu.encoding.sys` property is appended based
/// on the system-level ACP registry entry so the JVM uses the right encoding.
pub fn convert_vm_options(vm_options: Vec<String>) -> Result<Vec<CString>> {
    use windows::Win32::Globalization::{
        CP_ACP, CP_UTF8, GetACP, WC_NO_BEST_FIT_CHARS, WideCharToMultiByte,
    };
    use windows::core::{BOOL, HSTRING, PCSTR};

    let acp = unsafe { GetACP() };
    debug!("[ACP] process active code page = {acp}");

    let mut out: Vec<CString> = Vec::with_capacity(vm_options.len() + 1);

    for opt in &vm_options {
        let c = if acp == CP_UTF8 {
            CString::new(opt.as_bytes())
                .with_context(|| format!("invalid VM option string: {opt:?}"))?
        } else {
            // Convert UTF-8 → UTF-16 → ACP bytes.
            let wide = HSTRING::from(opt.as_str());
            let mut buf = vec![0u8; wide.len() * 4 + 1];
            let mut used_default = BOOL::default();
            let n = unsafe {
                WideCharToMultiByte(
                    CP_ACP,
                    WC_NO_BEST_FIT_CHARS,
                    &wide,
                    Some(&mut buf),
                    PCSTR::null(),
                    Some(&mut used_default),
                )
            };
            if n == 0 {
                bail!(
                    "cannot convert VM option {opt:?} to ACP {acp}: {}",
                    std::io::Error::last_os_error()
                );
            }
            if used_default.as_bool() {
                bail!("VM option {opt:?} is not representable in ACP {acp}");
            }
            buf.truncate(n as usize);
            CString::new(buf).with_context(|| format!("invalid ACP bytes for {opt:?}"))?
        };
        out.push(c);
    }

    // UTF-8 ACP: inject -Dsun.jnu.encoding.sys from the registry ACP value
    // so the JVM doesn't pick up the process UTF-8 mistakenly.
    if acp == CP_UTF8 {
        if let Some(sys_encoding) = read_system_acp_encoding() {
            debug!("[ACP] sys encoding = {sys_encoding}");
            let prop = format!("-Dsun.jnu.encoding.sys={sys_encoding}");
            out.push(CString::new(prop.as_bytes())?);
        }
    }

    Ok(out)
}

fn read_system_acp_encoding() -> Option<String> {
    use winreg::RegKey;
    use winreg::enums::*;

    let key = RegKey::predef(HKEY_LOCAL_MACHINE)
        .open_subkey("SYSTEM\\CurrentControlSet\\Control\\Nls\\CodePage")
        .ok()?;
    let acp: String = key.get_value("ACP").ok()?;
    Some(match acp.as_str() {
        "65001" => "UTF-8".to_string(),
        "1361" => "MS1361".to_string(),
        other => format!("windows-{other}"),
    })
}
