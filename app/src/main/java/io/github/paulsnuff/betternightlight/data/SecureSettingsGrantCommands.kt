package io.github.paulsnuff.betternightlight.data

object SecureSettingsGrantCommands {
    private const val PERMISSION = "android.permission.WRITE_SECURE_SETTINGS"

    /** Commands executed inside a root shell. */
    fun shellCommands(packageName: String): List<String> =
        listOf(
            "pm grant $packageName $PERMISSION",
            "/system/bin/pm grant $packageName $PERMISSION",
            "cmd package grant $packageName $PERMISSION",
            "/system/bin/cmd package grant $packageName $PERMISSION",
        )

    /** Commands executed by the Shizuku worker process. */
    fun workerCommands(packageName: String): List<Array<String>> =
        listOf(
            arrayOf("pm", "grant", packageName, PERMISSION),
            arrayOf("/system/bin/pm", "grant", packageName, PERMISSION),
            arrayOf("cmd", "package", "grant", packageName, PERMISSION),
            arrayOf("/system/bin/cmd", "package", "grant", packageName, PERMISSION),
        )
}
