package com.jonathaxs.gymnutshell.core.domain

/**
 * Quantidade usada só pra escolher a forma do plural (pluralStringResource / getQuantityString).
 *
 * A regra oficial de plurais (CLDR) trata o 0 como singular em português e francês ("0 dia").
 * No app o zero usa o plural ("0 dias"), igual ao iOS (chave "zero" do .stringsdict).
 * Com 2 todas as línguas do app caem na forma "other", que é o plural.
 * O número mostrado no texto continua vindo dos argumentos, não daqui.
 */
fun pluralQuantity(count: Int): Int = if (count == 0) 2 else count
