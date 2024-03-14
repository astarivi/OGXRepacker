mod img;
mod java;
mod read;
mod image;

use jni::JNIEnv;
use std::path::PathBuf;

use jni::objects::{JObject, JString};
use xdvdfs::write::img::ProgressInfo;

use jni::sys::jlongArray;
use std::sync::{mpsc, Arc};
use std::thread;
use tokio::runtime::Builder;

#[no_mangle]
pub extern "system" fn Java_ovh_astarivi_jxdvdfs_XDVDFS_pack<'local>(
    mut env: JNIEnv<'local>,
    object: JObject<'local>,
    source: JString<'local>,
    destination: JString<'local>,
) {
    let source_path: PathBuf = PathBuf::from(java::decode_string(&mut env, &source));
    let target_path: PathBuf = PathBuf::from(java::decode_string(&mut env, &destination));

    let (sender, receiver) = mpsc::channel();

    let result_handle = thread::spawn(|| {
        // We are using tokio for the dumbest reason ever
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

        let result = image::write::pack_img(source_path, target_path, progress_callback);

        rv.shutdown_background();

        result
    });

    while let Ok(data) = receiver.recv() {
        java::callback(&mut env, &object, data);
    }

    let result = result_handle.join();

    let return_value = match result {
        Ok(val) => val,
        Err(err) => {
            java::throw_exception(&mut env, format!("Threading error: {:?}", err));
            return;
        }
    };

    if let Err(err) = return_value {
        java::throw_exception(&mut env, format!("Packing error: {}", err));
    }
}

#[no_mangle]
pub extern "system" fn Java_ovh_astarivi_jxdvdfs_XDVDFS_unpack<'local>(
    mut env: JNIEnv<'local>,
    object: JObject<'local>,
    source: JString<'local>,
    destination: JString<'local>,
) {
    let source_str: String = java::decode_string(&mut env, &source);
    let destination_str: String = java::decode_string(&mut env, &destination);

    let (sender, receiver) = mpsc::channel();

    let result_handle = thread::spawn(move || {
        // We are using tokio for the dumbest reason ever
        let rv = Builder::new_multi_thread()
            .worker_threads(1)
            .build()
            .unwrap();

        let a_sender = Arc::new(sender);

        let progress_callback = |pi| match pi {
            ProgressInfo::DirAdded(path, sector) => {
                let s = a_sender.clone();
                rv.spawn(async move {
                    s.send(format!("Extracted dir: {:?} from sector {}", path, sector))
                        .unwrap();
                });
            }
            ProgressInfo::FileAdded(path, sector) => {
                let s = a_sender.clone();
                rv.spawn(async move {
                    s.send(format!("Extracted file: {:?} from sector {}", path, sector))
                        .unwrap();
                });
            }
            _ => {}
        };

        let result = read::unpack(&source_str, &destination_str, progress_callback);

        rv.shutdown_background();

        result
    });

    while let Ok(data) = receiver.recv() {
        java::unpack_callback(&mut env, &object, data);
    }

    let result = result_handle.join();

    let return_value = match result {
        Ok(val) => val,
        Err(err) => {
            java::throw_exception(&mut env, format!("Threading error: {:?}", err));
            return;
        }
    };

    if let Err(err) = return_value {
        java::throw_exception(&mut env, format!("Unpacking error: {}", err));
    }
}

#[no_mangle]
pub extern "system" fn Java_ovh_astarivi_jxdvdfs_XDVDFS_stat<'local>(
    mut env: JNIEnv<'local>,
    _object: JObject<'local>,
    source: JString<'local>,
) -> jlongArray {
    let source_path: PathBuf = PathBuf::from(java::decode_string(&mut env, &source));

    let result = read::stat(&source_path);

    return match result {
        Ok(val) => {
            let result_arr = env.new_long_array(2).expect("Failed to create return array");
            env.set_long_array_region(&result_arr, 0, &val)
                .expect("Failed to set return array region.");
            result_arr.into_raw()
        }
        Err(err) => {
            java::throw_exception(&mut env, format!("Image read error: {}", err));
            std::process::exit(1);
        }
    };
}

#[no_mangle]
pub extern "system" fn Java_ovh_astarivi_jxdvdfs_XDVDFS_ufile<'local>(
    mut env: JNIEnv<'local>,
    _object: JObject<'local>,
    source: JString<'local>,
    destination: JString<'local>,
    internal_file: JString<'local>,
) {
    let source_str: String = java::decode_string(&mut env, &source);
    let destination_str: String = java::decode_string(&mut env, &destination);
    let internal_file_str: String = java::decode_string(&mut env, &internal_file);

    let result = read::unpack_file(&source_str, &destination_str, &internal_file_str);

    if let Err(err) = result {
        java::throw_exception(&mut env, format!("Unpack error: {}", err));
    }
}
