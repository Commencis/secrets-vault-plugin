package YOUR_PACKAGE_NAME

class SECRETS_CLASS_NAME_PREFIXSecrets {
    companion object {
        init {
            System.loadLibrary("SECRETS_FILE_PREFIXsecrets")
        }
    }
}
