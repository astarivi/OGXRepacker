mod image;
mod java;

use jni::JNIEnv;
use std::path::PathBuf;

use crate::image::wrapper::SplitBufWriterWrapper;
use jni::objects::{JObject, JString};
use jni::sys::{jlong, jlongArray};
use splitfile::SplitFile;
use std::sync::mpsc;
use std::thread;

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
        let target_image = std::fs::File::options()
            .write(true)
            .truncate(true)
            .create(true)
            .open(target_path)?;

        let mut target_image = std::io::BufWriter::with_capacity(1024 * 1024, target_image);

        image::write::pack(&source_path, &mut target_image, sender)
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
pub extern "system" fn Java_ovh_astarivi_jxdvdfs_XDVDFS_packSplit<'local>(
    mut env: JNIEnv<'local>,
    object: JObject<'local>,
    source: JString<'local>,
    destination: JString<'local>,
    split_size: jlong,
) {
    let source_path: PathBuf = PathBuf::from(java::decode_string(&mut env, &source));
    let target_path: PathBuf = PathBuf::from(java::decode_string(&mut env, &destination));
    let volume_size = split_size as u64;

    let (sender, receiver) = mpsc::channel();

    let result_handle = thread::spawn(move || {
        let target_image = SplitFile::create(target_path, volume_size)?;

        let mut target_image =
            SplitBufWriterWrapper(std::io::BufWriter::with_capacity(1024 * 1024, target_image));

        image::write::pack(&source_path, &mut target_image, sender)
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
pub extern "system" fn Java_ovh_astarivi_jxdvdfs_XDVDFS_stat<'local>(
    mut env: JNIEnv<'local>,
    _object: JObject<'local>,
    source: JString<'local>,
) -> jlongArray {
    let source_path: PathBuf = PathBuf::from(java::decode_string(&mut env, &source));

    let result = image::read::stat(&source_path);

    match result {
        Ok(val) => {
            let result_arr = env
                .new_long_array(4)
                .expect("Failed to create return array");
            env.set_long_array_region(&result_arr, 0, &val)
                .expect("Failed to set return array region.");
            result_arr.into_raw()
        }
        Err(err) => {
            java::throw_exception(&mut env, format!("Image read error: {}", err));
            std::process::exit(1);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_ovh_astarivi_jxdvdfs_XDVDFS_fstat<'local>(
    mut env: JNIEnv<'local>,
    _object: JObject<'local>,
    source: JString<'local>,
    internal_file: JString<'local>,
) -> jlongArray {
    let source_str = java::decode_string(&mut env, &source);
    let internal_str = java::decode_string(&mut env, &internal_file);

    let result = image::read::file_offset(
        &source_str,
        &internal_str,
    );

    match result {
        Ok(val) => {
            let result_arr = env
                .new_long_array(2)
                .expect("Failed to create return array");
            env.set_long_array_region(&result_arr, 0, &val)
                .expect("Failed to set return array region.");
            result_arr.into_raw()
        }
        Err(err) => {
            java::throw_exception(&mut env, format!("File stat error: {}", err));
            std::process::exit(1);
        }
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
        image::read::unpack(&source_str, &destination_str, sender)
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

    let result = image::read::unpack_file(&source_str, &destination_str, &internal_file_str);

    if let Err(err) = result {
        java::throw_exception(&mut env, format!("Unpack error: {}", err));
    }
}
