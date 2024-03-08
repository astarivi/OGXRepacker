use jni::objects::{JObject, JString, JValue};
use jni::sys::jint;
use jni::JNIEnv;
use std::cell::RefMut;
use std::rc::Rc;

pub enum CallbackCodes {
    ExtractingFiles = 0,
    Finished = 1,
    Error = 2,
}

pub fn callback(
    mut env: RefMut<JNIEnv>,
    object: Rc<JObject>,
    message: String,
    status: CallbackCodes,
) {
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
