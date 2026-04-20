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
//! Single-instance IPC.
//!
//! Windows: named file mapping (16 KiB) + named auto-reset event.
//! First instance creates both, spawns a watcher thread that wakes on the
//! event and dispatches the forwarded command line into the JVM via
//! `WindowsCommandLineProcessor.processWindowsLauncherCommandLine`.
//! Second instance opens the mapping, writes `"cwd\ncmdline"`, signals the
//! event, and exits.
//!
//! Names: `ConsuloLauncherMapping.<sanitized-exe-path>` and
//!        `ConsuloLauncherEvent.<sanitized-exe-path>` — `:` and `\` → `_`.
//! Must match the convention used by the existing C++ LoaderLibrary so
//! running Consulo (old build) + starting Consulo (new build) for the same
//! install recognize each other.
//!
//! Non-Windows: always "I am the first"; no IPC in v1.

use anyhow::Result;
use std::path::Path;

/// Entry point. Call from the LoaderLibrary entry BEFORE loading the JVM.
///
/// Returns `Ok(true)` if this process is the first instance (should boot the JVM).
/// Returns `Ok(false)` if another instance exists (this process forwarded its
/// cmdline to the first instance and should exit).
pub fn check_and_register(exe_path: &Path) -> Result<bool> {
    #[cfg(not(target_os = "windows"))]
    {
        let _ = exe_path;
        Ok(true)
    }

    #[cfg(target_os = "windows")]
    {
        win::claim(exe_path)
    }
}

/// Notify the single-instance machinery that the JVM is now loaded.
/// On Windows (first instance only), spawns the watcher thread that dispatches
/// forwarded command lines into the running JVM. No-op elsewhere / for
/// second-instance processes.
#[allow(unused_variables)]
pub fn on_jvm_ready(jvm: *mut jni::sys::JavaVM) {
    #[cfg(target_os = "windows")]
    win::start_watcher(jvm);
}

// ============================================================================
// Windows implementation
// ============================================================================

#[cfg(target_os = "windows")]
mod win {
    use anyhow::{Result, bail};
    use log::{debug, error, info, warn};
    use std::ffi::{CString, c_void};
    use std::path::Path;
    use std::sync::atomic::{AtomicBool, AtomicPtr, Ordering};
    use std::sync::{Mutex, OnceLock};
    use std::thread;
    use std::time::Duration;

    use windows::Win32::Foundation::{CloseHandle, HANDLE, INVALID_HANDLE_VALUE};
    use windows::Win32::System::Memory::{
        CreateFileMappingA, FILE_MAP_ALL_ACCESS, MapViewOfFile, OpenFileMappingA,
        PAGE_READWRITE, UnmapViewOfFile,
    };
    use windows::Win32::System::Threading::{
        CreateEventA, INFINITE, SetEvent, WaitForSingleObject,
    };
    use windows::core::PCSTR;

    /// File-mapping size. Matches the old C++ value at WinLoaderLibrary/WinLauncher.cpp:43.
    const MAPPING_SIZE: u32 = 16_000;

    struct State {
        mapping: HANDLE,
        event: HANDLE,
        terminating: AtomicBool,
    }

    // HANDLE is `*mut c_void` which is !Send. We serialize access through a Mutex
    // and never dereference concurrently; wrap in a newtype to assert Send+Sync.
    unsafe impl Send for State {}
    unsafe impl Sync for State {}

    static STATE: OnceLock<Mutex<Option<State>>> = OnceLock::new();
    static JVM: AtomicPtr<jni::sys::JavaVM> = AtomicPtr::new(std::ptr::null_mut());

    pub fn claim(exe_path: &Path) -> Result<bool> {
        let (mapping_name, event_name) = make_names(exe_path);
        debug!("[SI] mapping={mapping_name:?} event={event_name:?}");

        let m_cstr = CString::new(mapping_name.clone())?;
        let e_cstr = CString::new(event_name.clone())?;

        // Auto-reset event — each SetEvent wakes exactly one waiter.
        let event = unsafe { CreateEventA(None, false, false, PCSTR(e_cstr.as_ptr() as *const u8))? };

        // Try to open existing mapping. If it opens, someone else is already running.
        let existing = unsafe {
            OpenFileMappingA(FILE_MAP_ALL_ACCESS.0, false, PCSTR(m_cstr.as_ptr() as *const u8))
        };

        match existing {
            Ok(h) if !h.is_invalid() => {
                info!("[SI] second instance — forwarding cmdline");
                send_command_line(h);
                let _ = unsafe { SetEvent(event) };
                let _ = unsafe { CloseHandle(h) };
                let _ = unsafe { CloseHandle(event) };
                Ok(false)
            }
            _ => {
                debug!("[SI] first instance — creating mapping");
                let mapping = unsafe {
                    CreateFileMappingA(
                        INVALID_HANDLE_VALUE,
                        None,
                        PAGE_READWRITE,
                        0,
                        MAPPING_SIZE,
                        PCSTR(m_cstr.as_ptr() as *const u8),
                    )
                }?;
                let state = State {
                    mapping,
                    event,
                    terminating: AtomicBool::new(false),
                };
                *STATE
                    .get_or_init(|| Mutex::new(None))
                    .lock()
                    .unwrap() = Some(state);
                Ok(true)
            }
        }
    }

    pub fn start_watcher(jvm: *mut jni::sys::JavaVM) {
        JVM.store(jvm, Ordering::Release);

        let (mapping, event) = match STATE.get().and_then(|m| {
            m.lock().ok().and_then(|g| {
                g.as_ref().map(|s| (s.mapping, s.event))
            })
        }) {
            Some(x) => x,
            None => {
                debug!("[SI] no state — not starting watcher (not first instance)");
                return;
            }
        };

        // HANDLE is *mut c_void (not Send); wrap so the closure is Send.
        let mapping = HandlePtr(mapping);
        let event = HandlePtr(event);
        let _ = thread::Builder::new()
            .name("consulo-single-instance".into())
            .spawn(move || watcher_loop(mapping, event));
    }

    // Send wrapper for HANDLE (*mut c_void).
    struct HandlePtr(HANDLE);
    unsafe impl Send for HandlePtr {}

    fn watcher_loop(mapping: HandlePtr, event: HandlePtr) {
        let mapping = mapping.0;
        let event = event.0;
        loop {
            let _ = unsafe { WaitForSingleObject(event, INFINITE) };

            if is_terminating() {
                debug!("[SI] watcher exiting on terminate signal");
                break;
            }

            let view = unsafe { MapViewOfFile(mapping, FILE_MAP_ALL_ACCESS, 0, 0, 0) };
            if view.Value.is_null() {
                warn!(
                    "[SI] MapViewOfFile failed: {}",
                    std::io::Error::last_os_error()
                );
                continue;
            }

            // The payload is `"cwd\ncmdline"` as UTF-16, NUL-terminated.
            let (cwd, cmdline) = parse_payload(view.Value as *const u16);
            let _ = unsafe { UnmapViewOfFile(view) };

            let jvm = JVM.load(Ordering::Acquire);
            if jvm.is_null() {
                warn!("[SI] wake-up with no JVM registered");
                continue;
            }

            if let Err(e) = dispatch_to_jvm(jvm, &cwd, &cmdline) {
                error!("[SI] dispatching forwarded cmdline failed: {e:?}");
            }
        }
    }

    fn is_terminating() -> bool {
        STATE
            .get()
            .and_then(|m| m.lock().ok())
            .and_then(|g| g.as_ref().map(|s| s.terminating.load(Ordering::Acquire)))
            .unwrap_or(false)
    }

    #[allow(dead_code)] // wired for future use; current flow exits via process::exit
    pub fn shutdown() {
        if let Some(lock) = STATE.get() {
            if let Ok(mut guard) = lock.lock() {
                if let Some(state) = guard.as_mut() {
                    state.terminating.store(true, Ordering::Release);
                    let _ = unsafe { SetEvent(state.event) };
                    // Give watcher a moment to notice.
                    thread::sleep(Duration::from_millis(50));
                    let _ = unsafe { CloseHandle(state.mapping) };
                    let _ = unsafe { CloseHandle(state.event) };
                }
                *guard = None;
            }
        }
    }

    fn parse_payload(view: *const u16) -> (Vec<u16>, Vec<u16>) {
        let mut cwd = Vec::new();
        let mut cmdline = Vec::new();
        let mut in_cmdline = false;
        let mut i = 0isize;
        loop {
            let c = unsafe { *view.offset(i) };
            if c == 0 {
                break;
            }
            i += 1;
            if c == b'\n' as u16 && !in_cmdline {
                in_cmdline = true;
                continue;
            }
            if in_cmdline {
                cmdline.push(c);
            } else {
                cwd.push(c);
            }
            if i as u32 >= MAPPING_SIZE / 2 {
                break;
            }
        }
        (cwd, cmdline)
    }

    fn send_command_line(mapping: HANDLE) {
        let view = unsafe { MapViewOfFile(mapping, FILE_MAP_ALL_ACCESS, 0, 0, 0) };
        if view.Value.is_null() {
            warn!("[SI] MapViewOfFile (second instance) failed");
            return;
        }

        let cwd = std::env::current_dir()
            .map(|p| p.to_string_lossy().into_owned())
            .unwrap_or_default();
        let cmdline = std::env::args().collect::<Vec<_>>().join(" ");
        let payload = format!("{cwd}\n{cmdline}");
        let utf16: Vec<u16> = payload.encode_utf16().chain(std::iter::once(0)).collect();

        let max = (MAPPING_SIZE as usize) / 2;
        let n = utf16.len().min(max);
        unsafe {
            std::ptr::copy_nonoverlapping(utf16.as_ptr(), view.Value as *mut u16, n);
        }
        let _ = unsafe { UnmapViewOfFile(view) };
    }

    fn dispatch_to_jvm(jvm: *mut jni::sys::JavaVM, cwd: &[u16], cmdline: &[u16]) -> Result<()> {
        use jni::sys::{JNIEnv, JNI_VERSION_21, JavaVMAttachArgs};

        let mut env: *mut c_void = std::ptr::null_mut();
        let name = CString::new("Consulo launcher cmdline thread")?;
        let mut attach_args = JavaVMAttachArgs {
            version: JNI_VERSION_21,
            name: name.as_ptr() as *mut _,
            group: std::ptr::null_mut(),
        };

        unsafe {
            let invoke = &(**jvm).v1_1;
            let rc = (invoke.AttachCurrentThread)(
                jvm,
                &mut env,
                &mut attach_args as *mut _ as *mut c_void,
            );
            if rc != jni::sys::JNI_OK {
                bail!("AttachCurrentThread failed: {rc}");
            }

            let env = env as *mut JNIEnv;
            let table = &(**env).v1_1;

            let class_name = CString::new("consulo/desktop/boot/main/windows/WindowsCommandLineProcessor")?;
            let class = (table.FindClass)(env, class_name.as_ptr());
            if class.is_null() {
                (table.ExceptionClear)(env);
                let _ = (invoke.DetachCurrentThread)(jvm);
                bail!("WindowsCommandLineProcessor class not found");
            }

            let m_name = CString::new("processWindowsLauncherCommandLine")?;
            let m_sig = CString::new("(Ljava/lang/String;Ljava/lang/String;)V")?;
            let method = (table.GetStaticMethodID)(env, class, m_name.as_ptr(), m_sig.as_ptr());
            if method.is_null() {
                (table.ExceptionClear)(env);
                let _ = (invoke.DetachCurrentThread)(jvm);
                bail!("processWindowsLauncherCommandLine method not found");
            }

            let j_cwd = (table.NewString)(env, cwd.as_ptr(), cwd.len() as jni::sys::jsize);
            let j_cmd = (table.NewString)(env, cmdline.as_ptr(), cmdline.len() as jni::sys::jsize);
            let args = [
                jni::sys::jvalue { l: j_cwd },
                jni::sys::jvalue { l: j_cmd },
            ];
            (table.CallStaticVoidMethodA)(env, class, method, args.as_ptr());
            if !(table.ExceptionOccurred)(env).is_null() {
                (table.ExceptionDescribe)(env);
                (table.ExceptionClear)(env);
            }
            let _ = (invoke.DetachCurrentThread)(jvm);
        }
        Ok(())
    }

    fn make_names(exe_path: &Path) -> (String, String) {
        let raw = exe_path.to_string_lossy().into_owned();
        let sanitized: String = raw
            .chars()
            .map(|c| if c == ':' || c == '\\' { '_' } else { c })
            .collect();
        (
            format!("ConsuloLauncherMapping.{sanitized}"),
            format!("ConsuloLauncherEvent.{sanitized}"),
        )
    }
}
