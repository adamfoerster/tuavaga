package com.adamfoerster.tuavaga.core.domain.condo

/** A condominium the user belongs to (only members can read it). */
data class Condominium(
    val id: String,
    val name: String,
    val address: String,
    val cep: String?,
    /** Blocks / towers offered when registering; empty = the condominium has no blocks. */
    val blocks: List<String>,
    val inviteCode: String,
)

enum class MembershipKind { RESIDENT, WORK }

/** The user's link to a condominium: where they live or work there. */
data class Membership(
    val condo: Condominium,
    val block: String?,
    val unit: String,
    val kind: MembershipKind,
)

/** A condominium found by name/address or invite code, before joining it. */
data class CondoPreview(
    val id: String,
    val name: String,
    val address: String,
    val blocksCount: Int,
    val listedSpots: Int,
    val isMember: Boolean,
    /** Known only when found by invite code; otherwise loaded when needed. */
    val blocks: List<String>? = null,
)

/** A garage level of a condominium the user belongs to ("Subsolo 2"), with its sectors in order. */
data class GarageLevel(val id: String, val name: String, val sectors: List<GarageSector>)

data class GarageSector(val id: String, val name: String)

/** Garage layout typed in "Cadastrar meu condomínio": levels in display order, each with its sectors. */
data class NewLevel(val name: String, val sectors: List<String>)

data class NewCondominium(
    val name: String,
    val address: String,
    val cep: String?,
    val blocks: List<String>,
    val levels: List<NewLevel>,
)

/** What the resident fills in "Seus dados no condomínio". */
data class ResidentInfo(
    val fullName: String,
    val phone: String?,
    val block: String?,
    val unit: String,
    val kind: MembershipKind,
)

/** The membership the app is showing: the stored one if still valid, otherwise the first. */
fun resolveActiveMembership(memberships: List<Membership>, activeCondoId: String?): Membership? =
    memberships.firstOrNull { it.condo.id == activeCondoId } ?: memberships.firstOrNull()
