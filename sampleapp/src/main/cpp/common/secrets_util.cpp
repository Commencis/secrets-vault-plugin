#include "secrets_util.hpp"

// Obfuscation key used to decode the obfuscated string
const char *obfuscatingStr = "chEYKrGb5PJx0I09oa1mlEuXE5FxPjX2";

/**
 * Decode the key using a custom logic.
 *
 * @param str The obfuscated key to be decoded
 * @return The decoded key
 */
char *customDecode(char *str) {
    /**
     * Implement your custom logic here.
     * Enhance the security of your key by encoding it prior to integrating it into the app.
     * Subsequently, employ your own logic to decode it within this function.
     */
    return str;
}

/**
 * Decodes the obfuscated string using the obfuscation key.
 *
 * @param obfuscatedStr The obfuscated string to be decoded
 * @param obfuscatedStrSize The size of the obfuscated string
 * @return The decoded string
 */
char *decode(const char *obfuscatedStr, int obfuscatedStrSize) {
    char buffer[2 * SHA256::DIGEST_SIZE + 1];
    // Generate the obfuscator key using SHA256
    sha256(obfuscatingStr, buffer);
    const char *obfuscator = buffer;

    // Allocate memory for the decoded string
    char *decodedStr = new char[obfuscatedStrSize + 1];

    // XOR the obfuscated string with the obfuscator key
    for (int i = 0; i < obfuscatedStrSize; i++) {
        decodedStr[i] = obfuscatedStr[i] ^ obfuscator[i % strlen(obfuscator)];
    }

    // Add null terminator to the decoded string
    decodedStr[obfuscatedStrSize] = '\0';
    return decodedStr;
}

/**
 * Retrieves the application context.
 *
 * @param pEnv A pointer to the JNI environment
 * @return The application context
 */
jobject getContext(JNIEnv *pEnv) {
    // Find the AppGlobals class
    jclass jAppAppGlobalsClass = pEnv->FindClass("android/app/AppGlobals");

    // Get the getInitialApplication method
    jmethodID jGetInitialApplication = pEnv->GetStaticMethodID(
        jAppAppGlobalsClass,
        "getInitialApplication",
        "()Landroid/app/Application;"
    );

    // Call the getInitialApplication method to get the Application object
    jobject jApplicationObject = pEnv->CallStaticObjectMethod(jAppAppGlobalsClass, jGetInitialApplication);

    return jApplicationObject;
}

/**
 * Retrieves the app signature.
 *
 * @param pEnv A pointer to the JNI environment
 * @return The app signature
 */
const char *getSignature(JNIEnv *pEnv) {
    // Get the Application context
    jobject context = getContext(pEnv);
    // Find the Build.VERSION class
    jclass versionClass = pEnv->FindClass("android/os/Build$VERSION");
    // Get the SDK_INT field
    jfieldID sdkIntFieldID = pEnv->GetStaticFieldID(versionClass, "SDK_INT", "I");
    // Get the SDK version
    int sdkInt = pEnv->GetStaticIntField(versionClass, sdkIntFieldID);
    // Find the Context class
    jclass contextClass = pEnv->FindClass("android/content/Context");
    // Get the getPackageManager method
    jmethodID pmMethod = pEnv->GetMethodID(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    // Call the getPackageManager method to get the PackageManager object
    jobject pm = pEnv->CallObjectMethod(context, pmMethod);
     // Find the PackageManager class
    jclass pmClass = pEnv->GetObjectClass(pm);
    // Get the getPackageInfo method
    jmethodID piMethod = pEnv->GetMethodID(
        pmClass,
        "getPackageInfo",
        "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;"
    );
    // Get the getPackageName method
    jmethodID pnMethod = pEnv->GetMethodID(contextClass, "getPackageName", "()Ljava/lang/String;");
    // Get the package name
    jstring packageName = (jstring) (pEnv->CallObjectMethod(context, pnMethod));

    // Set the flags based on the SDK version
    int flags;
    if (sdkInt >= 28) {
        flags = 0x08000000; // PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        flags = 0x00000040; // PackageManager.GET_SIGNATURES
    }

    // Call the getPackageInfo method to get the PackageInfo object
    jobject packageInfo = pEnv->CallObjectMethod(pm, piMethod, packageName, flags);
    // Find the PackageInfo class
    jclass piClass = pEnv->GetObjectClass(packageInfo);
    // Get the apkContentsSigners or signatures field, depending on the SDK version
    jobjectArray signatures;

    if (sdkInt >= 28) {
        // Get the signingInfo field
        jfieldID signingInfoField = pEnv->GetFieldID(piClass, "signingInfo", "Landroid/content/pm/SigningInfo;");
        jobject signingInfoObject = pEnv->GetObjectField(packageInfo, signingInfoField);
        // Find the SigningInfo class
        jclass signingInfoClass = pEnv->GetObjectClass(signingInfoObject);
        // Get the getApkContentsSigners method
        jmethodID signaturesMethod = pEnv->GetMethodID(
            signingInfoClass,
            "getApkContentsSigners",
            "()[Landroid/content/pm/Signature;"
        );
        // Call the getApkContentsSigners method to get the signatures
        signatures = (jobjectArray) (pEnv->CallObjectMethod(signingInfoObject, signaturesMethod));
    } else {
        // Get the signatures field
        jfieldID signaturesField = pEnv->GetFieldID(piClass, "signatures", "[Landroid/content/pm/Signature;");
        jobject signaturesObject = pEnv->GetObjectField(packageInfo, signaturesField);
        if (pEnv->IsSameObject(signaturesObject, NULL)) {
            return ""; // Return empty string if signatures is null
        }
        signatures = (jobjectArray) (signaturesObject);
    }

    // Get the first signature in the array
    jobject signature = pEnv->GetObjectArrayElement(signatures, 0);
    // Find the Signature class
    jclass signatureClass = pEnv->GetObjectClass(signature);
    // Get the toByteArray method
    jmethodID toByteArrayMethod = pEnv->GetMethodID(signatureClass, "toByteArray", "()[B");
    // Call the toByteArray method to get the signature byte array
    jobject signatureByteArray = (jobject) pEnv->CallObjectMethod(signature, toByteArrayMethod);
    // Find the MessageDigest class
    jclass mdClass = pEnv->FindClass("java/security/MessageDigest");
    // Get the getInstance method
    jmethodID mdMethod = pEnv->GetStaticMethodID(
        mdClass,
        "getInstance",
        "(Ljava/lang/String;)Ljava/security/MessageDigest;"
    );
    // Get an instance of the MD5 MessageDigest algorithm
    jobject md5Object = pEnv->CallStaticObjectMethod(mdClass, mdMethod, pEnv->NewStringUTF("MD5"));
    // Get the update method from the MessageDigest class
    jmethodID mdUpdateMethod = pEnv->GetMethodID(mdClass, "update", "([B)V");
    // Call the update method on the MD5 instance with the signature byte array
    pEnv->CallVoidMethod(md5Object, mdUpdateMethod, signatureByteArray);
    // Get the digest method from the MessageDigest class
    jmethodID mdDigestMethod = pEnv->GetMethodID(mdClass, "digest", "()[B");
    // Call the digest method on the MD5 instance to get the fingerprint byte array
    jbyteArray fingerprintByteArray = (jbyteArray)pEnv->CallObjectMethod(md5Object, mdDigestMethod);
    // Get the length of the fingerprint byte array
    jsize byteArrayLength = pEnv->GetArrayLength(fingerprintByteArray);
    // Get the elements of the fingerprint byte array
    jbyte *fingerprintByteArrayElements = pEnv->GetByteArrayElements(fingerprintByteArray, JNI_FALSE);
    // Convert the byte array to a char array
    char *charArray = (char *) fingerprintByteArrayElements;
    // Allocate memory for the md5 string
    char *md5 = (char *) calloc(2 * byteArrayLength + 1, sizeof(char));
    // Iterate over the char array and convert each byte to a hex string
    int k;
    for (k = 0; k < byteArrayLength; k++) {
        sprintf(&md5[2 * k], "%02X", (unsigned char) charArray[k]);
    }
    return md5;
}

/**
 * Checks if the app signature matches any of the expected app signatures
 *
 * @param appSignatures An array of expected app signatures (in encoded format)
 * @param size The size of the app signatures array
 * @param pEnv A pointer to the JNI environment
 *
 * @return true if the actual app signature matches any of the expected app signatures, false otherwise
 */
bool checkAppSignatures(const char appSignatures[][32], size_t size, JNIEnv *pEnv) {
    // Get the actual app signature of the package
    const char *actualAppSignature = getSignature(pEnv);
    // Iterate over the expected app signatures
    for(int i = 0; i < size; i++) {
        // Decode the expected app signature
        char *decodedAppSignature = decode(appSignatures[i], 32);
        // Compare the actual app signature with the expected app signature
        if (strcmp(decodedAppSignature, actualAppSignature) == 0) {
            // Release the memory allocated for the decoded app signature
            delete [] decodedAppSignature;
            // Return true if the actual app signature matches the expected app signature
            return true;
        }
        // Release the memory allocated for the decoded app signature
        delete [] decodedAppSignature;
    }
    // Return false if the actual app signature does not match any of the expected app signatures
    return false;
}

/**
 * Decodes the obfuscated secret and performs custom decoding
 *
 * @param obfuscatedSecret: The obfuscated secret
 * @param obfuscatedSecretSize: The size of the obfuscated secret
 * @param pEnv A pointer to the JNI environment
 *
 * @return The original key
 */
jstring getOriginalKey(
        char *obfuscatedSecret,
        int obfuscatedSecretSize,
        JNIEnv *pEnv) {
    const char appSignatures[][32] = {
        { 0x8, 0x23, 0x5c, 0x56, 0x7d, 0x54, 0x0, 0xc, 0x8, 0x7f, 0x53, 0x50, 0x73, 0x52, 0x27, 0x50, 0x52, 0x7, 0x72, 0x57, 0x4, 0x22, 0x20, 0x4, 0x2, 0x75, 0x6, 0x24, 0x51, 0x5, 0x7b, 0x7 },
        { 0xd, 0x57, 0x51, 0x21, 0x7f, 0x27, 0xd, 0xd, 0x8, 0xe, 0x21, 0x54, 0x2, 0x51, 0x27, 0x51, 0x53, 0x74, 0x6, 0x57, 0x74, 0x52, 0x20, 0x7, 0x5, 0x70, 0x77, 0x23, 0x50, 0x4, 0x8, 0x7 }
    };
    if (!checkAppSignatures(appSignatures, 2, pEnv)) {
        return pEnv->NewStringUTF("");
    }
    // Decode the obfuscated secret
    char *decodedSecret = decode(obfuscatedSecret, obfuscatedSecretSize);
    // Perform custom decoding on the decoded secret
    jstring originalKey = pEnv->NewStringUTF(customDecode(decodedSecret));
    // Release the memory allocated for the decoded secret
    delete [] decodedSecret;
    // Return the original key
    return originalKey;
}
