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
//! JNI bootstrap — load libjvm, call JNI_CreateJavaVM, invoke main class.
//!
//! Structure mirrors [XPlatLauncher java.rs]:
//!   1. Unix: reset SIGBUS/SIGSEGV/SIGINT before JVM installs handlers.
//!   2. Spawn a JVM thread (JNI spec forbids creating the VM on primordial).
//!   3. Inside the thread: load libjvm via libloading, get JNI_CreateJavaVM,
//!      build JavaVMInitArgs, create the VM, find `<variant::MAIN_CLASS>`,
//!      call its static `main([Ljava/lang/String;)V`.
//!   4. macOS: park the primordial on CFRunLoop.
//!      Other OSes: join the JVM thread.
//!
//! The JVM thread calls `std::process::exit` at the end — `launch` returns
//! only in the error path (JVM failed to start).

use crate::variant;
use anyhow::{Result, anyhow, bail};
use jni::sys::{
    JNI_OK, JNIEnv, JNINativeInterface_, JNI_VERSION_21, JavaVM,
    JavaVMInitArgs, JavaVMOption, jint, jsize, jvalue,
};
use libloading::{Library, Symbol};
use log::{debug, error};
use std::ffi::{CString, c_char, c_void};
use std::path::{Path, PathBuf};
use std::ptr;
use std::sync::mpsc;
use std::thread;

#[cfg(target_os = "windows")]
pub const JVM_LIB_REL: &str = "bin\\server\\jvm.dll";
#[cfg(target_os = "macos")]
pub const JVM_LIB_REL: &str = "lib/libjli.dylib";
#[cfg(target_os = "linux")]
pub const JVM_LIB_REL: &str = "lib/server/libjvm.so";

type JniCreateJavaVm = unsafe extern "system" fn(
    pvm: *mut *mut JavaVM,
    penv: *mut *mut c_void,
    args: *mut c_void,
) -> jint;

/// Everything the launcher needs to hand off to the JVM.
#[derive(Debug)]
pub struct LaunchSpec {
    pub jre_home: PathBuf,
    pub vm_options: Vec<String>,
    pub main_args: Vec<String>,
}

/// Load the JVM, invoke `<variant::MAIN_CLASS>.main(args)`.
///
/// Flow on the spawned JVM thread:
///   1. Load libjvm, call `JNI_CreateJavaVM`.
///   2. Send `Ok(())` on the bootstrap channel — the primordial thread
///      was blocked here; it now proceeds to run the event loop (CFRunLoop
///      on macOS; joining the JVM thread elsewhere).
///   3. Invoke `<main_class>.main(args)`. For GUI apps, `main()` returns
///      quickly after dispatching to the EDT.
///   4. Call `DestroyJavaVM`. **This is load-bearing:** it blocks until all
///      non-daemon Java threads (EDT, background workers) finish. Without
///      it, the process would exit as soon as `main()` returned — killing
///      the running IDE ~seconds after startup.
///   5. `std::process::exit(0)` once the JVM has truly shut down.
pub fn launch(spec: LaunchSpec) -> Result<()> {
    #[cfg(target_family = "unix")]
    reset_signal_handlers()?;

    let libjvm_path = spec.jre_home.join(JVM_LIB_REL);
    let main_class = variant::MAIN_CLASS.to_string();
    let main_args = spec.main_args.clone();
    let mut vm_options = spec.vm_options;

    // -Dsun.java.command is what jps/tools use to recognize the process.
    let mut command = main_class.clone();
    for arg in &main_args {
        command.push(' ');
        command.push_str(arg);
    }
    vm_options.push(format!("-Dsun.java.command={command}"));

    debug!("libjvm: {libjvm_path:?}");
    debug!("vm options ({}):", vm_options.len());
    for opt in &vm_options {
        debug!("  {opt}");
    }
    debug!("main class: {main_class}");
    debug!("main args: {main_args:?}");

    let (tx, rx) = mpsc::channel::<Result<()>>();

    let jvm_thread = thread::Builder::new()
        .name("consulo-jvm".into())
        .spawn(move || {
            match load_jvm(&libjvm_path, vm_options) {
                Ok((jvm, env)) => {
                    // Phase 1 done: signal the primordial thread so it can
                    // start the event loop (CFRunLoop on macOS).
                    let _ = tx.send(Ok(()));

                    // Notify the single-instance watcher (Windows only).
                    crate::single_instance::on_jvm_ready(jvm);

                    // Phase 2: invoke main. Returns quickly for GUI apps.
                    match call_main(env, &main_class, &main_args) {
                        Ok(()) => {
                            debug!("[JVM] main returned; waiting for non-daemon threads");
                            // Blocks until EDT and other non-daemon threads finish.
                            let invoke = unsafe { &(**jvm).v1_1 };
                            let rc = unsafe { (invoke.DestroyJavaVM)(jvm) };
                            debug!("[JVM] DestroyJavaVM returned {rc}");
                            std::process::exit(0);
                        }
                        Err(e) => {
                            error!("[JVM] main failed: {e:?}");
                            std::process::exit(1);
                        }
                    }
                }
                Err(e) => {
                    // Bootstrap failed; primordial unblocks and returns the error.
                    let reported = anyhow!("{e:#}");
                    let _ = tx.send(Err(reported));
                }
            }
        })?;

    // Wait for the JVM bootstrap to report success or failure.
    let bootstrap = rx.recv().map_err(|e| anyhow!("JVM thread disconnected: {e}"))?;
    bootstrap?;

    run_event_loop(jvm_thread)
}

#[cfg(target_os = "windows")]
fn setup_dll_search_path(jre_home: &Path) -> Result<()> {
    use windows::Win32::System::LibraryLoader;
    use windows::core::HSTRING;

    let flags =
        LibraryLoader::LOAD_LIBRARY_SEARCH_DEFAULT_DIRS | LibraryLoader::LOAD_LIBRARY_SEARCH_USER_DIRS;
    unsafe { LibraryLoader::SetDefaultDllDirectories(flags) }
        .map_err(|e| anyhow!("SetDefaultDllDirectories: {e}"))?;

    let jre_bin = jre_home.join("bin");
    debug!("AddDllDirectory {jre_bin:?}");
    let cookie = unsafe { LibraryLoader::AddDllDirectory(&HSTRING::from(jre_bin.as_path())) };
    if cookie.is_null() {
        return Err(anyhow!(
            "AddDllDirectory({}): {}",
            jre_bin.display(),
            std::io::Error::last_os_error()
        ));
    }
    Ok(())
}

#[cfg(target_family = "unix")]
fn reset_signal_handlers() -> Result<()> {
    for signal in [libc::SIGBUS, libc::SIGSEGV, libc::SIGINT] {
        unsafe {
            let mut action: libc::sigaction = std::mem::zeroed();
            action.sa_sigaction = libc::SIG_DFL;
            if libc::sigaction(signal, &action, ptr::null_mut()) != 0 {
                return Err(anyhow!(
                    "sigaction({}) failed: {}",
                    signal,
                    std::io::Error::last_os_error()
                ));
            }
        }
    }
    Ok(())
}

/// Load libjvm and create the JavaVM. Returns the raw (`JavaVM*`, `JNIEnv*`)
/// pointers for the caller to invoke methods on.
fn load_jvm(
    libjvm_path: &Path,
    vm_options: Vec<String>,
) -> Result<(*mut JavaVM, *mut JNIEnv)> {
    // Windows: configure the DLL search path so `jvm.dll`'s dependencies
    // (`msvcp140.dll`, `java.dll`, `awt.dll`, …) resolve from the JRE's bin
    // directory. Replaces the current-directory hack used by the C++ code.
    #[cfg(target_os = "windows")]
    {
        // libjvm_path is `<jre>/bin/server/jvm.dll`; jre home is three parents up.
        let jre_home = libjvm_path
            .parent()
            .and_then(|p| p.parent())
            .and_then(|p| p.parent())
            .ok_or_else(|| anyhow!("cannot derive JRE home from {libjvm_path:?}"))?;
        setup_dll_search_path(jre_home)?;
    }

    let libjvm = unsafe { Library::new(libjvm_path) }
        .map_err(|e| anyhow!("cannot load {libjvm_path:?}: {e}"))?;

    let create_vm: Symbol<'_, JniCreateJavaVm> = unsafe { libjvm.get(b"JNI_CreateJavaVM\0") }
        .map_err(|e| anyhow!("JNI_CreateJavaVM not found: {e}"))?;

    // Keep the CStrings alive for the duration of JNI_CreateJavaVM call.
    // Windows: convert each option to the process ACP; inject sys encoding when on UTF-8.
    #[cfg(target_os = "windows")]
    let c_options: Vec<CString> = crate::args_windows::convert_vm_options(vm_options.clone())?;

    #[cfg(not(target_os = "windows"))]
    let c_options: Vec<CString> = {
        let mut v = Vec::with_capacity(vm_options.len());
        for opt in &vm_options {
            v.push(CString::new(opt.as_str()).map_err(|e| anyhow!("invalid VM option {opt:?}: {e}"))?);
        }
        v
    };

    let mut jvm_options: Vec<JavaVMOption> = c_options
        .iter()
        .map(|c| JavaVMOption {
            optionString: c.as_ptr() as *mut c_char,
            extraInfo: ptr::null_mut(),
        })
        .collect();

    // Windows: install abort + vfprintf hooks so JVM native errors are captured
    // into the error dialog instead of lost to stderr.
    #[cfg(target_os = "windows")]
    let _hook_keepalive = {
        let (keepalive, hooks) = crate::jvm_hooks::hook_options();
        // Hooks go FIRST in the options array (before user VM options).
        let mut combined = hooks;
        combined.append(&mut jvm_options);
        jvm_options = combined;
        crate::jvm_hooks::start_capture();
        keepalive
    };

    let init_args = JavaVMInitArgs {
        version: JNI_VERSION_21,
        nOptions: jvm_options.len() as jint,
        options: jvm_options.as_mut_ptr(),
        ignoreUnrecognized: true,
    };

    let mut jvm: *mut JavaVM = ptr::null_mut();
    let mut env: *mut c_void = ptr::null_mut();

    debug!("calling JNI_CreateJavaVM");
    let rc = unsafe {
        create_vm(
            &mut jvm,
            &mut env,
            &init_args as *const _ as *mut c_void,
        )
    };
    debug!("JNI_CreateJavaVM returned {rc}");

    if rc != JNI_OK || jvm.is_null() || env.is_null() {
        #[cfg(target_os = "windows")]
        {
            let captured = crate::jvm_hooks::stop_capture().join("");
            if !captured.is_empty() {
                bail!("JNI_CreateJavaVM failed (rc={rc}):\n{captured}");
            }
        }
        bail!("JNI_CreateJavaVM failed: rc={rc}");
    }

    // JVM is up; stop capturing so later stderr flows normally through libc.
    #[cfg(target_os = "windows")]
    let _ = crate::jvm_hooks::stop_capture();

    // Keep libjvm alive — unloading it would kill the JVM.
    std::mem::forget(libjvm);

    Ok((jvm, env as *mut JNIEnv))
}

/// Invoke `<main_class>.main(String[])` via raw JNI.
///
/// `jni_sys` groups function-table members by JNI version introduced (`v1_1`,
/// `v1_2`, …); all the methods we use here are JNI 1.1, hence `.v1_1.*`.
fn call_main(env: *mut JNIEnv, main_class: &str, main_args: &[String]) -> Result<()> {
    unsafe {
        let table: *const JNINativeInterface_ = *env;
        let t = &(*table).v1_1;

        // FindClass for the main class.
        let class_name = CString::new(main_class.replace('.', "/"))?;
        let main_class_ref = (t.FindClass)(env, class_name.as_ptr());
        if main_class_ref.is_null() {
            (t.ExceptionDescribe)(env);
            (t.ExceptionClear)(env);
            bail!("main class not found: {main_class}");
        }

        // GetStaticMethodID main([Ljava/lang/String;)V
        let m_name = CString::new("main")?;
        let m_sig = CString::new("([Ljava/lang/String;)V")?;
        let main_method =
            (t.GetStaticMethodID)(env, main_class_ref, m_name.as_ptr(), m_sig.as_ptr());
        if main_method.is_null() {
            (t.ExceptionDescribe)(env);
            (t.ExceptionClear)(env);
            bail!("main method not found in {main_class}");
        }

        // FindClass java/lang/String
        let s_name = CString::new("java/lang/String")?;
        let string_class = (t.FindClass)(env, s_name.as_ptr());
        if string_class.is_null() {
            bail!("java.lang.String class not found");
        }

        // Build args array.
        let len = main_args.len() as jsize;
        let args_array = (t.NewObjectArray)(env, len, string_class, ptr::null_mut());
        if args_array.is_null() {
            bail!("could not allocate args array");
        }

        // Fill each element. Keep CStrings alive across the JNI calls.
        let mut c_args: Vec<CString> = Vec::with_capacity(main_args.len());
        for arg in main_args {
            c_args.push(
                CString::new(arg.as_str())
                    .map_err(|e| anyhow!("invalid arg {arg:?}: {e}"))?,
            );
        }
        for (i, c_arg) in c_args.iter().enumerate() {
            let j_str = (t.NewStringUTF)(env, c_arg.as_ptr());
            if j_str.is_null() {
                bail!("NewStringUTF failed for arg {i}");
            }
            (t.SetObjectArrayElement)(env, args_array, i as jsize, j_str);
        }

        // Call main.
        let call_args = [jvalue { l: args_array }];
        (t.CallStaticVoidMethodA)(env, main_class_ref, main_method, call_args.as_ptr());

        // Check and report any exception main threw.
        let exc = (t.ExceptionOccurred)(env);
        if !exc.is_null() {
            (t.ExceptionDescribe)(env);
            (t.ExceptionClear)(env);
            bail!("exception in {main_class}.main");
        }
    }
    Ok(())
}

// ===== Event loop =====

#[cfg(not(target_os = "macos"))]
fn run_event_loop(jvm_thread: thread::JoinHandle<()>) -> Result<()> {
    // The JVM thread calls std::process::exit at the end; this join never returns
    // under normal circumstances.
    jvm_thread
        .join()
        .map_err(|_| anyhow!("JVM thread panicked"))?;
    Ok(())
}

#[cfg(target_os = "macos")]
fn run_event_loop(_jvm_thread: thread::JoinHandle<()>) -> Result<()> {
    use core_foundation::base::{CFRelease, TCFTypeRef, kCFAllocatorDefault};
    use core_foundation::date::CFTimeInterval;
    use core_foundation::runloop::{
        CFRunLoopAddTimer, CFRunLoopGetCurrent, CFRunLoopRunInMode, CFRunLoopTimerCreate,
        CFRunLoopTimerRef, kCFRunLoopDefaultMode, kCFRunLoopRunFinished,
    };

    debug!("parking primordial thread on CFRunLoop");

    extern "C" fn timer_tick(_t: CFRunLoopTimerRef, _info: *mut std::ffi::c_void) {}

    unsafe {
        let timer = CFRunLoopTimerCreate(
            kCFAllocatorDefault,
            CFTimeInterval::MAX,
            0.0,
            0,
            0,
            timer_tick,
            ptr::null_mut(),
        );
        CFRunLoopAddTimer(CFRunLoopGetCurrent(), timer, kCFRunLoopDefaultMode);
        CFRelease(timer.as_void_ptr());
    }

    loop {
        let result =
            unsafe { CFRunLoopRunInMode(kCFRunLoopDefaultMode, CFTimeInterval::MAX, 0) };
        if result == kCFRunLoopRunFinished {
            break;
        }
    }
    Ok(())
}
