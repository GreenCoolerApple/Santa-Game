package com.greencoolerapple.santagame.lwjgl3

import com.badlogic.gdx.Files
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.greencoolerapple.santagame.SantaGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Santa Game")
        setWindowedMode(1280, 720)
        setResizable(true)
        useVsync(true)
        setForegroundFPS(0)
        setIdleFPS(30)
        setWindowIcon(Files.FileType.Internal, "icons/icon128.png", "icons/icon64.png", "icons/icon32.png", "icons/icon16.png")
    }
    Lwjgl3Application(SantaGame(), config)
}
