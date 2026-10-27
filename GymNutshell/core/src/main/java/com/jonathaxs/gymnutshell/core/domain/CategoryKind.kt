package com.jonathaxs.gymnutshell.core.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Tipo de categoria — porte de CategoryKind (iOS). Define o controle das metas dela na Hoje:
 * - [Standard]: slider ou − / + (estilo global).
 * - [RestDay]: igual ao padrão, mais o botão ON/OFF de dia de descanso.
 * - [CheckIn]: seletor Pendente | Feito. Feito grava o valor da meta; Pendente grava 0.
 */
enum class CategoryKind(val rawValue: String) {
    Standard("standard"),
    RestDay("restDay"),
    CheckIn("checkIn");

    companion object {
        /** Desconhecido (backup de versão futura) cai no padrão. */
        fun fromRaw(raw: String?): CategoryKind = entries.firstOrNull { it.rawValue == raw } ?: Standard

        /** Valor gravado ao marcar Feito/Pendente numa meta check-in. */
        fun checkInValue(done: Boolean, goal: Int): Int = if (done) maxOf(goal, 1) else 0

        /** Meta check-in conta como feita quando o valor alcança a meta. Valor parcial antigo = Pendente. */
        fun isCheckInDone(current: Int, goal: Int): Boolean = current > 0 && current >= goal
    }
}

/** Serializa pelo rawValue do iOS ("checkIn"); valor desconhecido vira [CategoryKind.Standard] em vez de quebrar o backup. */
object CategoryKindSerializer : KSerializer<CategoryKind> {
    override val descriptor = PrimitiveSerialDescriptor("CategoryKind", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: CategoryKind) = encoder.encodeString(value.rawValue)
    override fun deserialize(decoder: Decoder): CategoryKind = CategoryKind.fromRaw(decoder.decodeString())
}
