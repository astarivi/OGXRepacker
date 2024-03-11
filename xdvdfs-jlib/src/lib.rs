mod img;
mod java;
mod pack;
mod read;

use jni::JNIEnv;
use std::path::{Path, PathBuf};

use jni::objects::{JObject, JString};
use jni::strings::JavaStr;
use xdvdfs::write::img::ProgressInfo;

use std::sync::{mpsc, Arc};
use std::thread;
use anyhow::Error;
use jni::sys::{jint, jintArray};
use tokio::runtime::Builder;

#[no_mangle]
pub extern "system" fn Java_XDVDFS_pack<'local>(
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
                    java::CallbackCodes::Working,
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


#[no_mangle]
pub extern "system" fn Java_XDVDFS_stat<'local>(
    mut env: JNIEnv<'local>,
    object: JObject<'local>,
    source: JString<'local>
) -> jintArray {
    let data: Vec<jint> = vec![-1, -1];
    let failure_array = env.new_int_array(data.len() as i32).expect("Failed to create Java array");
    env.set_int_array_region(failure_array, 0, &data).expect("Failed to set array region");

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
                return failure_array.as_raw();
            }
        },
    ));

    let result = read::stat(&*source_path);

    return match result {
        Ok(val) => {
            let result_arr = env.new_int_array(val.len() as i32).expect("Failed to create return array");
            env.set_int_array_region(result_arr, 0, &val).expect("Failed to set return array region.");
            result_arr.as_raw()
        }
        Err(_) => {
            failure_array.as_raw()
        }
    }
}