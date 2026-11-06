package com.greencoolerapple.santagame.i18n

import java.util.Locale

enum class Language(val label: String) {
    ENGLISH("English"),
    PORTUGUESE_BR("Português (Brasil)");

    companion object {
        fun system(): Language =
            if (Locale.getDefault().language.equals("pt", ignoreCase = true)) PORTUGUESE_BR else ENGLISH
    }
}

object I18n {
    var language = Language.ENGLISH

    fun t(key: String, vararg args: Any): String {
        val table = when (language) {
            Language.ENGLISH -> english
            Language.PORTUGUESE_BR -> portuguese
        }
        var text = table[key] ?: english[key] ?: key
        for (i in args.indices) {
            text = text.replace("{$i}", args[i].toString())
        }
        return text
    }

    private val english = mapOf(
        "menu.gameMode" to "Game Mode",
        "menu.options" to "Options",
        "back" to "Back",
        "gameMode.title" to "GAME MODE",
        "mode.catch" to "Catch Gifts",
        "mode.stolen" to "Stolen Gifts",
        "mode.escape" to "Escape from the Santa City",
        "best" to "Best: {0}",
        "continueHouse" to "Continue: House {0}",
        "options.title" to "OPTIONS",
        "options.fps" to "FPS",
        "options.max" to "Max",
        "options.language" to "Language",
        "options.save" to "Save Game",
        "options.load" to "Load Game",
        "slots.save" to "SAVE GAME",
        "slots.load" to "LOAD GAME",
        "slot" to "Slot {0}",
        "slot.empty" to "Empty",
        "slot.damaged" to "Damaged save",
        "slot.catch" to "Catch {0}",
        "slot.stolen" to "Stolen {0}",
        "slot.escape" to "Escape {0}",
        "slot.house" to "House {0}",
        "toast.saved" to "Game saved to {0}",
        "toast.saveFailed" to "Could not save to {0}",
        "toast.empty" to "{0} is empty",
        "toast.damaged" to "{0} is damaged",
        "toast.loaded" to "{0} loaded",
        "toast.loadFailed" to "Could not load {0}",
        "pause.title" to "PAUSED",
        "pause.resume" to "Resume",
        "pause.menu" to "Menu",
        "over.retry" to "Retry",
        "over.menu" to "Menu",
        "over.newBest" to "NEW BEST!",
        "score" to "Score: {0}",
        "catch.gameOver" to "GAME OVER",
        "catch.statGifts" to "Gifts saved",
        "catch.statTime" to "Seconds",
        "stolen.gifts" to "Gifts: {0}",
        "stolen.house" to "House {0}",
        "stolen.hintSneak" to "Sneak to the tree and steal the gifts",
        "stolen.hintEscape" to "Escape through the fireplace!",
        "stolen.hintSack" to "Sack: {0}  -  Escape through the fireplace",
        "stolen.continue" to "Continuing from House {0}",
        "stolen.robbed" to "House robbed!",
        "stolen.gain" to "+{0} gifts",
        "stolen.caught" to "CAUGHT!",
        "stolen.statGifts" to "Gifts stolen",
        "stolen.statHouses" to "Houses robbed",
        "escape.city" to "City {0}",
        "escape.candy" to "Candy: {0}",
        "escape.hint" to "Find the exit and don't get caught!",
        "escape.exit" to "EXIT",
        "escape.escaped" to "You escaped the city!",
        "escape.bonus" to "+{0} points",
        "escape.wrapped" to "WRAPPED UP!",
        "escape.statCandy" to "Candies",
        "escape.statCities" to "Cities escaped",
        "escape.statScore" to "Score"
    )

    private val portuguese = mapOf(
        "menu.gameMode" to "Modos de Jogo",
        "menu.options" to "Opções",
        "back" to "Voltar",
        "gameMode.title" to "MODOS DE JOGO",
        "mode.catch" to "Pegar Presentes",
        "mode.stolen" to "Presentes Roubados",
        "mode.escape" to "Fuga da Cidade do Papai Noel",
        "best" to "Recorde: {0}",
        "continueHouse" to "Continuar: Casa {0}",
        "options.title" to "OPÇÕES",
        "options.fps" to "FPS",
        "options.max" to "Máx",
        "options.language" to "Idioma",
        "options.save" to "Salvar Jogo",
        "options.load" to "Carregar Jogo",
        "slots.save" to "SALVAR JOGO",
        "slots.load" to "CARREGAR JOGO",
        "slot" to "Espaço {0}",
        "slot.empty" to "Vazio",
        "slot.damaged" to "Save corrompido",
        "slot.catch" to "Pegar {0}",
        "slot.stolen" to "Roubo {0}",
        "slot.escape" to "Fuga {0}",
        "slot.house" to "Casa {0}",
        "toast.saved" to "Jogo salvo no {0}",
        "toast.saveFailed" to "Não foi possível salvar no {0}",
        "toast.empty" to "O {0} está vazio",
        "toast.damaged" to "O {0} está corrompido",
        "toast.loaded" to "{0} carregado",
        "toast.loadFailed" to "Não foi possível carregar o {0}",
        "pause.title" to "PAUSADO",
        "pause.resume" to "Continuar",
        "pause.menu" to "Menu",
        "over.retry" to "Jogar de novo",
        "over.menu" to "Menu",
        "over.newBest" to "NOVO RECORDE!",
        "score" to "Pontos: {0}",
        "catch.gameOver" to "FIM DE JOGO",
        "catch.statGifts" to "Presentes salvos",
        "catch.statTime" to "Segundos",
        "stolen.gifts" to "Presentes: {0}",
        "stolen.house" to "Casa {0}",
        "stolen.hintSneak" to "Chegue de fininho na árvore e roube os presentes",
        "stolen.hintEscape" to "Fuja pela lareira!",
        "stolen.hintSack" to "Saco: {0}  -  Fuja pela lareira",
        "stolen.continue" to "Continuando da Casa {0}",
        "stolen.robbed" to "Casa roubada!",
        "stolen.gain" to "+{0} presentes",
        "stolen.caught" to "PEGO!",
        "stolen.statGifts" to "Presentes roubados",
        "stolen.statHouses" to "Casas roubadas",
        "escape.city" to "Cidade {0}",
        "escape.candy" to "Doces: {0}",
        "escape.hint" to "Ache a saída e não seja pego!",
        "escape.exit" to "SAÍDA",
        "escape.escaped" to "Você fugiu da cidade!",
        "escape.bonus" to "+{0} pontos",
        "escape.wrapped" to "EMBRULHADO!",
        "escape.statCandy" to "Doces",
        "escape.statCities" to "Cidades escapadas",
        "escape.statScore" to "Pontuação"
    )
}
