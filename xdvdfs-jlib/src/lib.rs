mod img;
mod java;
mod pack;

use jni::JNIEnv;
use std::path::PathBuf;

use jni::objects::{JObject, JString};
use jni::strings::JavaStr;
use xdvdfs::write::img::ProgressInfo;

use std::sync::{mpsc, Arc};
use std::thread;
use tokio::runtime::Builder;

#[no_mangle]
pub extern "system" fn Java_XDVDFSImpl_pack<'local>(
    mut env: JNIEnv<'local>,
    object: JObject<'local>,
    source: JString<'local>,
    destination: JString<'local>,
) {
    let source_path: PathBuf = PathBuf::from(<JavaStr<'_, '_, '_> as Into<String>>::into(
        match env.get_string(&source) {
            Ok(java_string) => java_string,
            Err(err) => {
                java::callback(
                    &mut env,
                    &object,
                    format!("Error while decoding input path: {}", err),
                    java::CallbackCodes::Error,
                );
                return;
            }
        },
    ));

    let target_path: PathBuf = PathBuf::from(<JavaStr<'_, '_, '_> as Into<String>>::into(
        match env.get_string(&destination) {
            Ok(java_string) => java_string,
            Err(err) => {
                java::callback(
                    &mut env,
                    &object,
                    format!("Error while decoding output path: {}", err),
                    java::CallbackCodes::Error,
                );
                return;
            }
        },
    ));

    let (sender, receiver) = mpsc::channel();

    let result_handle = thread::spawn(|| {
        // We're using tokio for the dumbest reason ever
        let rv = Builder::new_multi_thread()
            .worker_threads(1)
            .build()
            .unwrap();

        let a_sender = Arc::new(sender);

        let progress_callback = |pi| match pi {
            ProgressInfo::DirAdded(path, sector) => {
                let s = a_sender.clone();
                rv.spawn(async move {
                    s.send(format!("Added dir: {:?} at sector {}", path, sector))
                        .unwrap();
                });
            }
            ProgressInfo::FileAdded(path, sector) => {
                let s = a_sender.clone();
                rv.spawn(async move {
                    s.send(format!("Added file: {:?} at sector {}", path, sector))
                        .unwrap();
                });
            }
            _ => {}
        };

        let result = pack::pack_img(source_path, target_path, progress_callback);

        rv.shutdown_background();

        result
    });

    loop {
        match receiver.recv() {
            Ok(data) => {
                java::callback(
                    &mut env,
                    &object,
                    data,
                    java::CallbackCodes::ExtractingFiles,
                );
            }
            Err(_) => {
                println!("Sender has been closed");
                break;
            }
        }
    }

    let result = result_handle.join();

    let return_value = match result {
        Ok(val) => val,
        Err(err) => {
            println!("Error occurred: {:?}", err);
            return;
        }
    };

    if let Err(err) = return_value {
        java::callback(
            &mut env,
            &object,
            format!("An error has occurred: {}", err),
            java::CallbackCodes::Error,
        );
        std::process::exit(1);
    }

    java::callback(
        &mut env,
        &object,
        String::from("All done"),
        java::CallbackCodes::Finished,
    );
}
