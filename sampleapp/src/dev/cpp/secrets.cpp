#include "../../main/cpp/common/secrets_util.cpp"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_commencis_secretsvaultplugin_sampleapp_Secrets_a0(
        JNIEnv* pEnv,
        jobject pThis) {
     char obfuscatedSecret[] = { 0x5a, 0xd, 0x8, 0x9, 0x56, 0xd, 0x7e, 0x59, 0x51, 0x4f, 0xd, 0x14, 0x66, 0x7, 0xf, 0x16, 0x3, 0x0, 0x73, 0x0, 0x44 };
     return getOriginalKey(obfuscatedSecret, sizeof(obfuscatedSecret), pEnv);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_commencis_secretsvaultplugin_sampleapp_Secrets_a1(
        JNIEnv* pEnv,
        jobject pThis) {
     char obfuscatedSecret[] = { 0x5d, 0x7, 0x13, 0x2b, 0x57, 0xf, 0x41, 0x63, 0x51, 0x55, 0x17, 0x3, 0x1 };
     return getOriginalKey(obfuscatedSecret, sizeof(obfuscatedSecret), pEnv);
}
