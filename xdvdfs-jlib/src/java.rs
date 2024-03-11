use jni::objects::{JObject, JString, JValue};
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

pub fn throw_exception(env: &mut JNIEnv, message: String) {
    env.throw_new("ovh/astarivi/jxdvdfs/base/XDVDFSException", message)
        .expect("Aborting!");
}
