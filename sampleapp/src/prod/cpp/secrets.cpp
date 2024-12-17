#include "../../main/cpp/common/secrets_util.cpp"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_commencis_secretsvaultplugin_sampleapp_Secrets_a0(
        JNIEnv* pEnv,
        jobject pThis) {
     char obfuscatedSecret[] = { 0x5a, 0xd, 0x8, 0x9, 0x56, 0xd, 0x7e, 0x59, 0x51, 0x4f, 0xd, 0x14, 0x66, 0x7, 0xf, 0x16, 0x3, 0x0, 0x67, 0x17, 0x5d, 0x2 };
     return getOriginalKey(obfuscatedSecret, sizeof(obfuscatedSecret), pEnv);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_commencis_secretsvaultplugin_sampleapp_Secrets_a1(
        JNIEnv* pEnv,
        jobject pThis) {
     char obfuscatedSecret[] = { 0x49, 0x10, 0xa, 0x0, 0x76, 0xd, 0x54, 0x4c, 0x66, 0x58, 0xe, 0x13, 0x55, 0x57 };
     return getOriginalKey(obfuscatedSecret, sizeof(obfuscatedSecret), pEnv);
}
