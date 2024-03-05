mod img;
mod pack;
mod java;

use std::cell::RefCell;
use std::path::PathBuf;
use std::rc::Rc;

use jni::JNIEnv;

use jni::objects::{JString, JObject};
use jni::strings::JavaStr;
use xdvdfs::write::img::ProgressInfo;


#[no_mangle]
pub extern "system" fn Java_XDVDFSImpl_pack<'local>(env: JNIEnv<'local>,
                                                    object: JObject<'local>,
                                                    source: JString<'local>,
                                                    destination: JString<'local>
                                                    ){
    // Real dumb solution right here
    let env_refcell = Rc::new(RefCell::new(env));
    let obj_rc = Rc::new(object);
    let local_obj = Rc::clone(&obj_rc);

    let source_path: PathBuf = PathBuf::from(
        <JavaStr<'_, '_, '_> as Into<String>>::into(
            match Rc::clone(&env_refcell).borrow_mut().get_string(&source) {
                Ok(java_string) => java_string,
                Err(err) => {
                    java::callback(
                        Rc::clone(&env_refcell).borrow_mut(),
                        local_obj,
                        format!("Error while decoding input path: {}", err),
                        java::CallbackCodes::Error
                    );
                    return
                }
            }
        )
    );

    let target_path: PathBuf = PathBuf::from(
        <JavaStr<'_, '_, '_> as Into<String>>::into(
            match Rc::clone(&env_refcell).borrow_mut().get_string(&destination) {
                Ok(java_string) => java_string,
                Err(err) => {
                    java::callback(
                        Rc::clone(&env_refcell).borrow_mut(),
                        local_obj,
                        format!("Error while decoding output path: {}", err),
                        java::CallbackCodes::Error
                    );
                    return
                }
            }
        )
    );

    let progress_callback = |pi| match pi {
        ProgressInfo::DirAdded(path, sector) => {
            java::callback(
                Rc::clone(&env_refcell).borrow_mut(),
                Rc::clone(&obj_rc),
                format!("Added dir: {:?} at sector {}", path, sector),
                java::CallbackCodes::ExtractingFiles
            );
        }
        ProgressInfo::FileAdded(path, sector) => {
            java::callback(
                Rc::clone(&env_refcell).borrow_mut(),
                Rc::clone(&obj_rc),
                format!("Added file: {:?} at sector {}", path, sector),
                java::CallbackCodes::ExtractingFiles
            );
        }
        _ => {}
    };

    let result = pack::pack_img(source_path, target_path, progress_callback);

    if let Err(err) = result {
        java::callback(
            Rc::clone(&env_refcell).borrow_mut(),
            Rc::clone(&obj_rc),
            format!("An error has occurred: {}", err),
            java::CallbackCodes::Error
        );
        std::process::exit(1);
    }

    java::callback(
        Rc::clone(&env_refcell).borrow_mut(),
        Rc::clone(&obj_rc),
        String::from("All done"),
        java::CallbackCodes::Finished
    );
}
