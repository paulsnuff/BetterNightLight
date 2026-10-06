package io.github.paulsnuff.betternightlight.shizuku

import android.os.IBinder
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

internal object ColorDisplayBinder {
    /**
     * Applies [kelvin] through IColorDisplayManager, transacted by the Shizuku server
     * (shell/root uid) via ShizukuBinderWrapper. The reflective proxy lookup needs a
     * hidden-API exemption.
     */
    fun setKelvin(kelvin: Int): Boolean = runCatching {
        HiddenApiBypass.setHiddenApiExemptions("Landroid/hardware/display/IColorDisplayManager")
        val raw = SystemServiceHelper.getSystemService("color_display") ?: return false
        val service = Class.forName("android.hardware.display.IColorDisplayManager\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, ShizukuBinderWrapper(raw))
        service.javaClass
            .getMethod("setNightDisplayColorTemperature", Int::class.javaPrimitiveType)
            .invoke(service, kelvin) as Boolean
    }.getOrDefault(false)
}
