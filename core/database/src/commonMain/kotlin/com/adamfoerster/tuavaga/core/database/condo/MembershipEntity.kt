package com.adamfoerster.tuavaga.core.database.condo

import androidx.room3.Entity

/** Cached membership of a user (condominium data denormalized), so the app opens offline. */
@Entity(tableName = "membership", primaryKeys = ["userId", "condoId"])
data class MembershipEntity(
    val userId: String,
    val condoId: String,
    val condoName: String,
    val address: String,
    val cep: String?,
    /** Blocks joined with '\n'; empty string = no blocks. */
    val blocks: String,
    val inviteCode: String,
    val block: String?,
    val unit: String,
    /** "morador" | "trabalho", as stored in Supabase. */
    val kind: String,
    /** Order in which the backend returned it (by join date). */
    val position: Int,
)
