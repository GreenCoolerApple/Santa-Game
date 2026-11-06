package com.greencoolerapple.santagame.ios

import com.badlogic.gdx.backends.iosrobovm.IOSApplication
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration
import com.greencoolerapple.santagame.SantaGame
import org.robovm.apple.foundation.NSAutoreleasePool
import org.robovm.apple.uikit.UIApplication

class IOSLauncher : IOSApplication.Delegate() {
    override fun createApplication(): IOSApplication {
        val configuration = IOSApplicationConfiguration().apply {
            orientationPortrait = false
            orientationLandscape = true
            useAccelerometer = false
            useCompass = false
            useHaptics = true
            hideHomeIndicator = true
            preventScreenDimming = true
        }
        return IOSApplication(SantaGame(), configuration)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val pool = NSAutoreleasePool()
            val principalClass: Class<UIApplication>? = null
            UIApplication.main(args, principalClass, IOSLauncher::class.java)
            pool.close()
        }
    }
}
