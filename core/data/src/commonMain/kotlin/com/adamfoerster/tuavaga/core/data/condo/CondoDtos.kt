package com.adamfoerster.tuavaga.core.data.condo

import com.adamfoerster.tuavaga.core.database.condo.MembershipEntity
import com.adamfoerster.tuavaga.core.domain.condo.CondoPreview
import com.adamfoerster.tuavaga.core.domain.condo.Condominium
import com.adamfoerster.tuavaga.core.domain.condo.GarageLevel
import com.adamfoerster.tuavaga.core.domain.condo.GarageSector
import com.adamfoerster.tuavaga.core.domain.condo.Membership
import com.adamfoerster.tuavaga.core.domain.condo.MembershipKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CondoDto(
    val id: String,
    val name: String,
    val address: String,
    val cep: String? = null,
    val blocks: List<String> = emptyList(),
    @SerialName("invite_code") val inviteCode: String,
)

/** Row of `memberships` with the condominium embedded (PostgREST resource embedding). */
@Serializable
internal data class MembershipDto(
    val block: String? = null,
    val unit: String,
    val kind: String,
    @SerialName("condominiums") val condo: CondoDto,
)

/** Row returned by `search_condominiums` / `find_condominium_by_invite`. */
@Serializable
internal data class CondoPreviewDto(
    val id: String,
    val name: String,
    val address: String,
    val blocks: List<String>? = null,
    @SerialName("blocks_count") val blocksCount: Int,
    @SerialName("listed_spots") val listedSpots: Int,
    @SerialName("is_member") val isMember: Boolean,
)

/** Row of `condo_levels` with its sectors embedded. */
@Serializable
internal data class LevelDto(
    val id: String,
    val name: String,
    val position: Int,
    @SerialName("condo_sectors") val sectors: List<SectorDto> = emptyList(),
)

@Serializable
internal data class SectorDto(val id: String, val name: String, val position: Int)

internal fun LevelDto.toGarageLevel() = GarageLevel(
    id = id,
    name = name,
    sectors = sectors.sortedBy { it.position }.map { GarageSector(it.id, it.name) },
)

internal const val KIND_RESIDENT = "morador"
internal const val KIND_WORK = "trabalho"

internal fun MembershipKind.toDb(): String = when (this) {
    MembershipKind.RESIDENT -> KIND_RESIDENT
    MembershipKind.WORK -> KIND_WORK
}

internal fun kindFromDb(value: String): MembershipKind =
    if (value == KIND_WORK) MembershipKind.WORK else MembershipKind.RESIDENT

internal fun MembershipDto.toEntity(userId: String, position: Int) = MembershipEntity(
    userId = userId,
    condoId = condo.id,
    condoName = condo.name,
    address = condo.address,
    cep = condo.cep,
    blocks = condo.blocks.joinToString("\n"),
    inviteCode = condo.inviteCode,
    block = block,
    unit = unit,
    kind = kind,
    position = position,
)

internal fun MembershipEntity.toMembership() = Membership(
    condo = Condominium(
        id = condoId,
        name = condoName,
        address = address,
        cep = cep,
        blocks = if (blocks.isEmpty()) emptyList() else blocks.split("\n"),
        inviteCode = inviteCode,
    ),
    block = block,
    unit = unit,
    kind = kindFromDb(kind),
)

internal fun CondoPreviewDto.toPreview() = CondoPreview(
    id = id,
    name = name,
    address = address,
    blocksCount = blocksCount,
    listedSpots = listedSpots,
    isMember = isMember,
    blocks = blocks,
)
