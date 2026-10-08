package com.adamfoerster.tuavaga.feature.messages.presentation.di

import com.adamfoerster.tuavaga.core.domain.time.appNow
import com.adamfoerster.tuavaga.feature.messages.presentation.chat.ChatViewModel
import com.adamfoerster.tuavaga.feature.messages.presentation.list.ConversationsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val messagesPresentationModule = module {
    viewModelOf(::ConversationsViewModel)
    // Route arguments come via parametersOf (see MessagesNavigation).
    viewModel { (bookingId: String) -> ChatViewModel(bookingId, get(), get(), now = { appNow() }) }
}
