package com.adamfoerster.tuavaga.feature.messages.data.di

import com.adamfoerster.tuavaga.feature.messages.data.SupabaseMessagesRepository
import com.adamfoerster.tuavaga.feature.messages.domain.MessagesRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val messagesDataModule = module {
    single { SupabaseMessagesRepository(get(), get(), get(), get()) } bind MessagesRepository::class
}
