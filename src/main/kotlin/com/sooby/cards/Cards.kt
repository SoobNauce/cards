@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.sooby.cards

class Cards {
    enum class Color{BLACK, RED}
    enum class Suit(val color: Color) {
        CLUBS(Color.BLACK),
        DIAMONDS(Color.RED),
        HEARTS(Color.RED),
        SPADES(Color.BLACK),
        BLACK_JOKER(Color.BLACK),
        RED_JOKER(Color.RED);
    }
    enum class Value{
        ACE,
        TWO, THREE, FOUR, FIVE, SIX,
        SEVEN, EIGHT, NINE, TEN,
        JACK, QUEEN, KING, JOKER
    }
    class Card(val suit: Suit, val value: Value){
        override fun toString(): String =
            if(this.value == Value.JOKER){
                when(this.suit.color){
                    Color.BLACK -> "BLACK JOKER"
                    Color.RED -> "RED JOKER"
                    //else -> this.suit.color.toString() + " JOKER"
                }
            }else{
                "${this.value} OF ${this.suit}"
            }
    }
    // Card to card comparison is not defined in Cards but is rather defined within each game.
    companion object {
        val normalSuits = Suit.values().filter {
            (it != Suit.BLACK_JOKER) and (it != Suit.RED_JOKER)
        }
        val normalValues = Value.values().filter {
            it != Value.JOKER
        }

        fun jokers(): List<Card> = listOf(
            Card(Suit.BLACK_JOKER, Value.JOKER),
            Card(Suit.RED_JOKER, Value.JOKER)
        )

        fun normalCards() = normalSuits.flatMap { suit ->
            normalValues.map { value ->
                Card(suit, value)
            }
        }

        fun allWithJokers(): List<Card> = jokers() + normalCards()

        /**
        Creates a new deck of cards. The order is arbitrary and the deck
        must be shuffled first.
         */
        fun deckWithJokers(): ArrayDeque<Card> = ArrayDeque(allWithJokers())
        fun deckWithoutJokers(): ArrayDeque<Card> = ArrayDeque(normalCards())
    }
}
