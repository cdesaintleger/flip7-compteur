package io.github.cdesaintleger.flip7compteur

fun cardResource(id: String): Int = when(id) {
    "0" -> R.drawable.card_0
    "1" -> R.drawable.card_1
    "2" -> R.drawable.card_2
    "3" -> R.drawable.card_3
    "4" -> R.drawable.card_4
    "5" -> R.drawable.card_5
    "6" -> R.drawable.card_6
    "7" -> R.drawable.card_7
    "8" -> R.drawable.card_8
    "9" -> R.drawable.card_9
    "10" -> R.drawable.card_10
    "11" -> R.drawable.card_11
    "12" -> R.drawable.card_12
    "13" -> R.drawable.card_13
    "unlucky7" -> R.drawable.card_unlucky7
    "lucky13" -> R.drawable.card_lucky13
    "minus2" -> R.drawable.card_minus2
    "minus4" -> R.drawable.card_minus4
    "minus6" -> R.drawable.card_minus6
    "minus8" -> R.drawable.card_minus8
    "minus10" -> R.drawable.card_minus10
    "half" -> R.drawable.card_half
    else -> error("Carte inconnue : $id")
}

fun cardLabel(id: String): String = when(id) {
    "unlucky7" -> "7 porte-malheur"
    "lucky13" -> "13 porte-bonheur"
    "half" -> "Diviser par deux"
    "double" -> "Multiplier les numéros par deux"
    else -> if(id.startsWith("plus")) "Bonus plus ${id.removePrefix("plus")}" else if(id.startsWith("minus")) "Malus moins ${id.removePrefix("minus")}" else "Carte $id"
}
