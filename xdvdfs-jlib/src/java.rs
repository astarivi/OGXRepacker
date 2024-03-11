use jni::objects::{JObject, JString, JValue};
use jni::strings::JavaStr;
use jni::JNIEnv;

pub fn callback(env: &mut JNIEnv, object: &JObject, message: String) {
    let output: JString = env
        .new_string(message)
        .expect("Couldn't create callback message!");

    env.call_method(
        object,
        "pack_callback",
        "(Ljava/lang/String;)V",
        &[JValue::Object(&JObject::from(output))],
    )
    .expect("Failed to call callback Java method");
}

pub fn unpack_callback(env: &mut JNIEnv, object: &JObject, message: String) {
    let output: JString = env
        .new_string(message)
        .expect("Couldn't create callback unpack message!");

    env.call_method(
        object,
        "unpack_callback",
        "(Ljava/lang/String;)V",
        &[JValue::Object(&JObject::from(output))],
    )
    .expect("Failed to call callback Java method");
}

pub fn throw_exception(env: &mut JNIEnv, message: String) {
    env.throw_new("ovh/astarivi/jxdvdfs/base/XDVDFSException", message)
        .expect("Aborting!");
}

pub fn decode_string(env: &mut JNIEnv, j_string: &JString) -> String {
    return <JavaStr<'_, '_, '_> as Into<String>>::into(match env.get_string(j_string) {
        Ok(java_string) => java_string,
        Err(err) => {
            throw_exception(env, format!("Error while decoding param string: {}", err));
            std::process::exit(1);
        }
    });
}
