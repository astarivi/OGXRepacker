use jni::objects::{JObject, JString, JValue};
use jni::sys::jint;
use jni::JNIEnv;

pub enum CallbackCodes {
    Working = 0,
    Finished = 1,
    Error = 2,
}

pub fn callback(env: &mut JNIEnv, object: &JObject, message: String, status: CallbackCodes) {
    let output: JString = env
        .new_string(message)
        .expect("Couldn't create callback message!");

    env.call_method(
        object,
        "callback",
        "(Ljava/lang/String;I)V",
        &[
            JValue::Object(&JObject::from(output)),
            JValue::Int(status as jint),
        ],
    )
    .expect("Failed to call callback Java method");
}

pub fn throw_exception() {

}