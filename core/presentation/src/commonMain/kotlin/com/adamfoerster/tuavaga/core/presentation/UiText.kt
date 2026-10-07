package com.adamfoerster.tuavaga.core.presentation

import com.adamfoerster.tuavaga.core.domain.util.DataError

/**
 * Text produced outside of composables (ViewModels, mappers). Plain strings for now;
 * when the design/i18n lands this is where Compose string resources plug in.
 */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    fun asString(): String = when (this) {
        is Dynamic -> value
    }
}

fun DataError.toUiText(): UiText = UiText.Dynamic(
    when (this) {
        DataError.Remote.NO_INTERNET -> "Sem conexão com a internet."
        DataError.Remote.REQUEST_TIMEOUT -> "O servidor demorou para responder. Tente novamente."
        DataError.Remote.TOO_MANY_REQUESTS -> "Muitas tentativas. Aguarde um pouco e tente novamente."
        DataError.Remote.UNAUTHORIZED -> "Sua sessão expirou. Entre novamente."
        DataError.Remote.SERVER_ERROR -> "Erro no servidor. Tente novamente mais tarde."
        DataError.Remote.UNKNOWN -> "Algo deu errado. Tente novamente."
        DataError.Local.DISK_FULL -> "Sem espaço no dispositivo."
        DataError.Local.UNKNOWN -> "Erro ao salvar dados no dispositivo."
    },
)
