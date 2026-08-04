package com.subkan.core.model

/**
 * A payment method the user files subscriptions under. "Card" is the user's word for it; nothing
 * here is a card number — [colorHex] and a name are all the app stores.
 *
 * [colorHex] is six hex digits with no `#` and no alpha, e.g. `BF0000`. Storing the string rather
 * than a packed colour int keeps the domain free of `androidx.compose.ui.graphics.Color`.
 */
data class PaymentCard(
    val id: String,
    val name: String,
    val colorHex: String,
    val sortOrder: Int,
)

/**
 * The cards a fresh install starts with.
 *
 * Seeded rather than left empty because a subscription cannot be saved without a card, so an empty
 * app would open on a form the user cannot complete. They are ordinary rows — renameable,
 * recolourable and deletable like any other.
 */
val defaultPaymentCards: List<PaymentCard> = listOf(
    PaymentCard(id = "c1", name = "楽天カード", colorHex = "BF0000", sortOrder = 0),
    PaymentCard(id = "c2", name = "三井住友カード", colorHex = "004D40", sortOrder = 1),
    PaymentCard(id = "c3", name = "JCB", colorHex = "1976D2", sortOrder = 2),
    PaymentCard(id = "c4", name = "Visa", colorHex = "1A1F71", sortOrder = 3),
    PaymentCard(id = "c5", name = "Mastercard", colorHex = "FF5F00", sortOrder = 4),
    PaymentCard(id = "c6", name = "American Express", colorHex = "002663", sortOrder = 5),
    PaymentCard(id = "c7", name = "Diners Club", colorHex = "005596", sortOrder = 6),
)

/** The swatches offered in the card editor. The first is the default for a new card. */
val presetCardColors: List<String> = listOf(
    "BF0000", "004D40", "1976D2", "1A1F71", "FF5F00",
    "002663", "005596", "333333", "9C27B0", "E91E63",
)
