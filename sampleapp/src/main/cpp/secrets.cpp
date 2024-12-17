#include "common/secrets_util.cpp"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_commencis_secretsvaultplugin_sampleapp_MainSecrets_a0(
        JNIEnv* pEnv,
        jobject pThis) {
     char obfuscatedSecret[] = { 0x5e, 0x7, 0xb, 0x1, 0x4b, 0x2, 0x54, 0x63, 0x51, 0x55, 0x17, 0x3, 0x1 };
     return getOriginalKey(obfuscatedSecret, sizeof(obfuscatedSecret), pEnv);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_commencis_secretsvaultplugin_sampleapp_MainSecrets_a1(
        JNIEnv* pEnv,
        jobject pThis) {
     char obfuscatedSecret[] = { 0x5e, 0x7, 0xb, 0x1, 0x4b, 0x2, 0x54, 0x63, 0x51, 0x55, 0x17, 0x3, 0x2 };
     return getOriginalKey(obfuscatedSecret, sizeof(obfuscatedSecret), pEnv);
}
