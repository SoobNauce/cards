@file:Suppress("MemberVisibilityCanBePrivate", "RedundantVisibilityModifier", "unused")

package com.sooby.cards.eights
import com.sooby.cards.Cards.*
import com.sooby.cards.CanSummarize
import com.sooby.cards.Cards.Companion as CardsCompanion

class Eights {
    object StaticRules {
        /** Wild values require a suit declaration */
        var wildValues = listOf(Value.EIGHT, Value.JOKER)
        /** Special values require some special handling
         * such as a suit declaration (wild),
         * reversing direction (ace),
         * or skipping the next player (queen)
        */
        // QUEEN and ACE are special values, but dissimilar enough
        // that AI needs to be smart enough to handle them without help
        // from StaticRules.
        fun fullDeck() = CardsCompanion.deckWithJokers()
        /** There is no formal comparison on cards in this game.
         * This is only for displaying a player's hand.
         */
        fun cardKey(c: Card): Int = fullDeck().indexOfFirst {
            (it.value == c.value) and (it.suit == c.suit)
        }
    }
    class Attempt(val card: Card, declaredSuit: Suit? = null){
        val suit: Suit = declaredSuit.let {
            if (card.value in StaticRules.wildValues) {
                require(it != null) {
                    "A suit must be declared for wild values"
                }
                it
            } else {
                require(card.suit in CardsCompanion.normalSuits) {
                    "Unexpected joker-suited card $card, jokers supposedly ruled out above."
                }
                card.suit
            }
        }
        val value: Value = card.value
        override fun toString() =
            if(StaticRules.wildValues.contains(card.value)){
                "$card ($suit)"
            }else{card.toString()}
    }
    abstract class Player(val name: String){
        var hand = ArrayDeque<Card>()
        var game: CanSummarize? = null
        public fun registerGame(g: CanSummarize){
            this.game = g
        }
        fun accept(card: Card){
            hand.add(card)
        }

        /**
         * Describes cards for the benefit of a human player.
         */
        fun showHand(): String =
            hand.sortedBy(StaticRules::cardKey).mapIndexed { i: Int, c: Card ->
                when (i) {
                    0 -> c.toString()
                    hand.size - 1 -> "and $c"
                    else -> c.toString()
                }
            }.joinToString(", ")

        /**
         * Every player knows how many cards every other player is holding.
         */
        fun handSize(): Int = hand.size

        /**
         * Declare a card, or declare that you have no valid card to play.
         */
        abstract fun selectCard(
            lastCard: Card,
            lastSuit: Suit,
            lastValue: Value
        ): Card?

        /**
         * When playing a wildcard, declare a suit. It is unacceptable
         * to be unable to decide on a suit.
         */
        abstract fun declareSuit(
            lastCard: Card,
            lastSuit: Suit,
            lastValue: Value
        ): Suit

        /**
         * Players must be able to decide on a suit on turn 1
         * when no cards have been played.
         */
        abstract fun declareSuit(): Suit


        /**
         * Decide on a card to play. If playing a wildcard, declare
         * a suit for it as well.
         */
        abstract fun play(
            lastCard: Card,
            lastSuit: Suit,
            lastValue: Value
        ): Attempt?
    }
    /*class HumanPlayer(name: String): Player(name) {

    }*/
    class BasicAIPlayer(name: String): Player(name) {
        fun normalHand(): List<Card> = hand.filter{
            !StaticRules.wildValues.contains(it.value)
        }
        fun specialHand(): List<Card> = hand.filter{
            StaticRules.wildValues.contains(it.value)
        }
        /**
         * Organizes cards in hand by suit.
         */
        fun handBySuit(): Map<Suit, List<Card>> {
            val result: MutableMap<Suit, List<Card>> = mutableMapOf()
            for(card_e in normalHand()){
                val curCards: List<Card> = result.getOrDefault(card_e.suit, listOf())
                result[card_e.suit] = curCards.plus(card_e)
            }
            return result
        }
        fun handByValue(): Map<Value, List<Card>>{
            val result: MutableMap<Value, List<Card>> = mutableMapOf()
            for(card_e in normalHand()){
                val curCards: List<Card> = result.getOrDefault(card_e.value, listOf())
                result[card_e.value] = curCards.plus(card_e)
            }
            return result
        }
        @Suppress("unused")
        fun bestValue(): Value{
            val hValues = handByValue()
            val result: Value? = hValues.maxByOrNull {
                it.value.size
            }?.key
            require(result != null){
                "[$name]: Failed to find value with maximum number of cards in my hand"
            }
            return result
        }

        /**
         * Given a suit, finds the card whose value
         * matches the most other cards in hand.
         */
        fun bestCardOfSuit(s: Suit): Card? {
            val suitMatches = handBySuit().getOrDefault(s, listOf())
            val hValues = handByValue()
            return suitMatches.maxByOrNull {
                hValues.getOrDefault(it.value, listOf()).size
            }
        }
        fun bestCardOfValue(v: Value): Card? {
            val valueMatches = handByValue().getOrDefault(v, listOf())
            val hSuits = handBySuit()
            return valueMatches.maxByOrNull {
                hSuits.getOrDefault(it.suit, listOf()).size
            }
        }

        /**
         * Finds the card (by exact equality) in the player's hand.
         * We don't need search by value because we have other ways
         * (filter) of searching hand for a given card.
         */
        fun findCard(c: Card): Card? = this.hand.firstOrNull{
            it === c
        }

        fun searchAndRemove(c: Card): Card? =
            findCard(c)?.also { this.hand.remove(it) }
        // Override functions from Player
        /**
         * Basic AI doesn't care what other players have.
         * When picking a suit,
         * it simply picks the suit it has the most cards of.
         */
        override fun declareSuit(): Suit{
            val hSuits = handBySuit()
            val result: Suit? = hSuits.maxByOrNull{it.value.size}?.key
            // I have only wild cards
            if(hand.size == specialHand().size){
                return listOf(
                    Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES
                ).random()
            }
            require(result != null){
                "[$name]: I have non wild cards, but I couldn't decide what suit I have " +
                "the most of?"
            }
            return result
        }
        override fun declareSuit(
            lastCard: Card,
            lastSuit: Suit,
            lastValue: Value
        ) = declareSuit()

        override fun selectCard(
            lastCard: Card,
            lastSuit: Suit,
            lastValue: Value
        ): Card? = sequence {
            if(!StaticRules.wildValues.contains(lastValue)){
                yield(bestCardOfValue(lastValue))
            }
            yield(bestCardOfSuit(lastSuit))
            yield(specialHand().firstOrNull())
        }.firstOrNull{it != null}.also {
            if(it != null){
                require(searchAndRemove(it) != null){
                    "[$name]: A card was selected, but I couldn't" +
                    "remove it from my hand."
                }
            }
        }
        override fun play(
            lastCard: Card,
            lastSuit: Suit,
            lastValue: Value
        ): Attempt? {
            val c = selectCard(lastCard, lastSuit, lastValue)
            return if(c == null){
                null
            }else if(StaticRules.wildValues.contains(c.value)){
                Attempt(c, declareSuit(lastCard, lastSuit, lastValue))
            }else{
                Attempt(c, null)
            }
        }
    }
    class Game(initialPlayers: List<Player>): CanSummarize{
        init {
            require(initialPlayers.size > 1){
                "At least two players are required for this game."
            }
        }
        var players = initialPlayers.toMutableList().onEach {
            it.registerGame(this)
        }
        var cPlayerIndex: Int = 0
        val lastPlayerIndex: Int
            get() = players.size - 1
        var reversed = false
        var startingCards: Int = 5
        // Players are removed from `players` when they win
        // and added to `winners`.
        var winners: MutableList<Player> = mutableListOf()
        var loser: Player? = null
        // this is in fact a functional statement
        fun getCPlayer() = players[cPlayerIndex]

        /**
         * Returns the next player index after i
         */
        fun nextFromIndex(i: Int): Int =
            if(reversed){
                if(i - 1 < 0){
                    lastPlayerIndex
                }else{
                    i - 1
                }
            }else{
                if(i + 1 > lastPlayerIndex){
                    0
                }else{
                    i + 1
                }
            }
        fun nextSeveral(i: Int, depth: Int): List<Int> {
            var c = i
            val result: MutableList<Int> = mutableListOf(c)
            (0..depth).forEach{_ ->
                c = nextFromIndex(c)
                result.add(c)
            }
            return result
        }

        /**
         * The convention for cards in the deck is as follows:
         * Top (first) , ... , Bottom (last)
         * Cards are dealt from the top of the deck (first).
         */
        val nDecks = (players.size / 5) + 1
        var deck = ArrayDeque<Card>().also{
            (1..nDecks).forEach{_ ->
                it.addAll(StaticRules.fullDeck())
            }
        }
        //var deck = Eights.StaticRules.fullDeck().also{it.shuffle()}

        /**
         * As with the deck, the convention is as follows:
         * Top (first) , ... , Bottom (last)
         * Players play cards onto the top (first). When reshuffling, all but
         * the top ("first", but most recent) card are removed.
         */
        var history: ArrayDeque<Card> = ArrayDeque()
        var detailedHistory: MutableList<String> = mutableListOf("Game began.")

        /**
         * Attempts to reshuffle to get a card to deal.
         * Throws an error if, after attempting to reshuffle,
         * there are not enough cards to deal at least one card to a player.
         * This error can be caught and the player can simply be
         * prevented from drawing a card i.e. by losing their turn.
         */
        private fun reshuffle(){
            while(history.size > 1){// Always leaves at least one card
                deck.addLast(history.removeLast())
            }
            require(deck.size > 1){
                detailedHistory.add("Failed to reshuffle.")
                "Not enough cards left to redeal after reshuffling"
            }
            deck.shuffle()
            detailedHistory.add("Stack reshuffled into deck.")
        }
        fun dealOne(player: Player, suppressHistory: Boolean = false){
            if(deck.size > 0){
                player.accept(deck.removeFirst())
                if(!suppressHistory){
                    detailedHistory.add("Card dealt to ${player.name}.")
                }
            }else{
                reshuffle()
                require(deck.size >= 1){
                    // Failure to deal a card cannot be suppressed.
                    detailedHistory.add("Failed to deal after reshuffle.")
                    "Tried to reshuffle, but there are still no cards to deal"
                }
                player.accept(deck.removeFirst())
            }
        }
        fun dealSeveral(player: Player, n: Int) {
            detailedHistory.add("Dealt $n cards to ${player.name}.")
            (1..n).forEach{_ ->
                // No error handling here because we want the error to cascade.
                dealOne(player, suppressHistory = true)
            }
        }
        fun safeDeal(player: Player, n: Int = 1) {
            try{
                if(n == 1){
                    dealOne(player)
                }else{
                    dealSeveral(player, n)
                }
            }catch(e: IllegalArgumentException){
                detailedHistory.add("Skipping this draw due to insufficient cards.")
            }
        }

        var lastSuit: Suit// these are initialized in the init block below
        var lastValue: Value
        init{
            detailedHistory.add("Starting hand ($startingCards cards) dealt to players.")
            for(player in players){
                // Failure to deal at this stage is a consistency error.
                dealSeveral(player, startingCards)
            }
            val firstPlay = deck.removeFirst()
            detailedHistory.add("First card played from deck ($firstPlay).")
            history.addFirst(firstPlay)
            // ARBITRARY RULES DECISION: If the first card is a joker,
            // Rather than redealing, the first player declares its suit.
            if(StaticRules.wildValues.contains(firstPlay.value)){
                lastSuit = getCPlayer().declareSuit()
                lastValue = firstPlay.value
                detailedHistory.add("${getCPlayer().name} declared suit for joker: $lastSuit.")
            }else{
                lastSuit = firstPlay.suit
                lastValue = firstPlay.value
            }
        }
        fun constraints(): Triple<Card, Suit, Value> = Triple(
            history.last(), lastSuit, lastValue
        )
        fun possiblePlays(p: Player): List<Card>{
            return p.hand.filter{
                (StaticRules.wildValues.contains(it.value)
                or (it.value == lastValue)
                or (it.suit == lastSuit)
                )
            }
        }

        // Functions that update state
        /**
         * Finds the next player (by turn order) who has not won the game.
         * Returns null if at most one player remains without winning.
         */
        fun nextNonWinner(): Player?{
            var i = nextFromIndex(cPlayerIndex)
            while(i != cPlayerIndex){
                if(players[i].handSize() == 0){
                    i = nextFromIndex(i)
                }else{
                    return players[i]
                }
            }
            return null
        }

        /**
         * Update state to reflect the next player.
         */
        fun advancePlayer(skip: Boolean = false){
            // Find the next player who has not won
            val np = nextNonWinner()
            // Remove all winners
            // Find winners first
            // The call to toList() is because we're going to iterate over this later
            fun winners() = players.filter{it.hand.size == 0}.toList()
            winners().forEach{p ->
                    // Removing an element shifts all subsequent elements to the left
                    // The only reason we need to care about the index is if
                    // we're removing the player who occurs last in the list.
                    // And even then, we only need to care if it's that player's turn.
                    detailedHistory.add(
                        "${p.name} has won the game and will be removed."
                    )
                    winners.add(p)
                    players.remove(p)
                }
            // Now that winners have been removed, advance to the next player.
            when (players.size) {
                0 -> {
                    detailedHistory.add(
                        "All players have won. The game will end."
                    )
                    cPlayerIndex = -1
                    return
                }
                1 -> {
                    // If there is only one player remaining, they have lost.
                    val p = players.first()
                    loser = p
                    players.removeFirst()
                    cPlayerIndex = -1
                    detailedHistory.add(
                        "${p.name} has lost. The game will end."
                    )
                }
                else -> {
                    cPlayerIndex = if(skip){
                        nextFromIndex(players.indexOf(np))
                    }else {
                        players.indexOf(np)
                    }
                }
            }
        }

        /**
         * Ensures a given move is valid.
         * If a player's move is not valid, they should be dealt
         * a penalty card (in another function).
         */
        fun validateAttempt(a: Attempt) = (
                StaticRules.wildValues.contains(a.value)
                or (a.value == lastValue)
                or (a.suit == lastSuit)
        )

        /**
         * Validates attempt `a` generated by player `p` before accepting.
         * If the move is not valid, deals the card in `a` back to the player
         * along with an additional penalty card.
         */
        fun acceptAttempt(a: Attempt, p: Player){
            require(!p.hand.contains(a.card)){
                "${p.name} attempted to play a card (${a.card}) without removing it " +
                "from their hand. This should not be possible."
            }
            if(validateAttempt(a)) {
                lastSuit = a.suit
                lastValue = a.value
                history.addFirst(a.card)
                detailedHistory.add(
                    "${p.name} played ${a.card}. Next card must be $lastSuit or $lastValue."
                )
                if(p.hand.size == 0){
                    detailedHistory.add(
                        "${p.name} has zero cards and will be declared a winner."
                    )
                }
            } else {
                // We know `a.card` is not null
                // We need to give it back to `p`.
                p.accept(a.card)
                detailedHistory.add(
                    "Returned ${a.card} to ${p.name}. Penalty of 1 card applied.")
                // If a penalty cannot be applied, just skip the deal.
                safeDeal(p)
            }
        }

        /**
         * If a player decides to draw, no validation is performed.
         * A card is simply dealt to them. A player cannot request
         * more than one card per round.
         */
        fun acceptNull(p: Player){
            detailedHistory.add(
                "${p.name} has chosen to draw. Penalty of 1 card applied."
            )
            // We don't need to worry about infinite loops.
            // If a player tries to draw and there aren't enough cards
            // even after reshuffling, they simply lose their turn.
            safeDeal(p)
        }

        fun removeLoser(p: Player){
            detailedHistory.add(
                "${p.name} has lost the game. The game is over."
            )
            loser = players.first()
            players.removeFirst()
        }

        fun runTurn(){
            require(loser == null){
                "A loser has already been declared. There can be no more play."
            }
            if(players.size == 1){
                removeLoser(players.first())
                return
            }
            val p = getCPlayer()
            require(p.handSize() > 0){
                "${p.name} has a hand size of zero but has not already won?"
            }
            val a = p.play(
                history.first(),
                lastSuit,
                lastValue
            )
            if(a != null){
                // It's in advancePlayer() that winners are detected.
                acceptAttempt(a, p)
            }else{
                acceptNull(p)
            }
            val skip = when (a?.card?.value) {
                Value.QUEEN -> {
                    true
                }
                Value.ACE -> {
                    reversed = !reversed
                    (players.size == 2)
                }
                else -> {
                    false
                }
            }
            // Winners are checked in advancePlayer
            // which should always be called here.
            advancePlayer(skip)
        }
        fun runAll(){
            while(players.size > 1){
                runTurn()
            }
            if(players.size == 1){
                val p = players.first()
                loser = p
            }
        }


        // Summary functions
        override fun playersSummary() =
            listOf(
                players.joinToString("\n") {
                "${it.name} (player) (${it.handSize()} cards)"},
                winners.joinToString("\n"){
                "${it.name} (winner) (${it.handSize()} cards)"},
                (loser?.let {
                "${it.name} (loser) (${it.handSize()} cards)"} ?: "")
            ).joinToString("\n")

        override fun playerNames(): List<String> =
            (players.map{it.name}
                    + winners.map{it.name}
                    + listOfNotNull(loser?.name)
            )

        override fun historySummary(depth: Int) = detailedHistory.takeLast(depth)

        override fun nonPlayerSummary() = """Stack (${history.size} cards): Top card is ${history.first()}.
            |Next card must match $lastValue or $lastSuit.
            |${deck.size} cards in deck.
            |""".trimMargin("|")

        override fun predictNextPlayer() = players[nextFromIndex(cPlayerIndex)].toString()
        override fun predictNextPlayers(depth: Int) =
            nextSeveral(cPlayerIndex, depth).map{players[it].name}
    }
}
