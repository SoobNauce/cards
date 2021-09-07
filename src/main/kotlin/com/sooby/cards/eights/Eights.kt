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
        var specialValues = wildValues + listOf(
            Value.QUEEN, Value.ACE
        )
        fun fullDeck() = CardsCompanion.deckWithJokers()
        /** There is no formal comparison on cards in this game.
         * This is only for displaying a player's hand.
         */
        fun cardKey(c: Card): Int = fullDeck().indexOfFirst {it: Card ->
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
                "${card} ($suit)"
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
            hand.sortedBy(Eights.StaticRules::cardKey).mapIndexed { i: Int, c: Card ->
                when (i) {
                    0 -> c.toString()
                    hand.size - 1 -> "and ${c.toString()}"
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
        ): Eights.Attempt?
    }
    /*class HumanPlayer(name: String): Player(name) {

    }*/
    class BasicAIPlayer(name: String): Player(name) {
        fun normalHand(): List<Card> = hand.filter{
            !Eights.StaticRules.wildValues.contains(it.value)
        }
        fun specialHand(): List<Card> = hand.filter{
            Eights.StaticRules.wildValues.contains(it.value)
        }
        /**
         * Organizes cards in hand by suit.
         */
        fun handBySuit(): Map<Suit, List<Card>> {
            var result: MutableMap<Suit, List<Card>> = mutableMapOf()
            for(card_e in normalHand()){
                var curCards: List<Card> = result.getOrDefault(card_e.suit, listOf())
                result[card_e.suit] = curCards.plus(card_e)
            }
            return result
        }
        fun handByValue(): Map<Value, List<Card>>{
            var result: MutableMap<Value, List<Card>> = mutableMapOf()
            for(card_e in normalHand()){
                var curCards: List<Card> = result.getOrDefault(card_e.value, listOf())
                result[card_e.value] = curCards.plus(card_e)
            }
            return result
        }
        fun bestValue(): Value{
            var hValues = handByValue()
            var result: Value? = hValues.maxByOrNull {
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
        fun findCard(s: Suit, v: Value): Card? = this.hand.firstOrNull{
            (it.suit == s) and (it.value == v)
        }
        fun findCard(c: Card): Card? = this.hand.firstOrNull{
            (it.suit == c.suit) and (it.value == c.value)
        }
        fun searchAndRemove(s: Suit, v: Value): Card? =
            findCard(s, v)?.also { this.hand.remove(it) }
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
        }.firstOrNull {it != null}.also {
            if(it != null){
                require(searchAndRemove(it) != null){
                    "[$name]: A card was found, but I couldn't find it in my hand " +
                    "to remove it."
                }
            }
        }
        override fun play(
            lastCard: Card,
            lastSuit: Suit,
            lastValue: Value
        ): Eights.Attempt? {
            val c = selectCard(lastCard, lastSuit, lastValue)
            return if(c == null){
                null
            }else if(Eights.StaticRules.wildValues.contains(c.value)){
                Eights.Attempt(c, declareSuit(lastCard, lastSuit, lastValue))
            }else{
                Eights.Attempt(c, null)
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
            var result: MutableList<Int> = mutableListOf(c)
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
            (1..n).forEach{
                // No error handling here because we want the error to cascade.
                dealOne(player, suppressHistory = true)
            }
        }

        var lastSuit: Suit// these are initialized in the init block below
        var lastValue: Value
        init{
            detailedHistory.add("Starting hand ($startingCards cards) dealt to players.")
            for(player in players){
                dealSeveral(player, startingCards)
            }
            var firstPlay = deck.removeFirst()
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
        fun advancePlayer(skip: Boolean = false): Unit{
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
                    if(skip){
                        cPlayerIndex = nextFromIndex(players.indexOf(np))
                    }else {
                        cPlayerIndex = players.indexOf(np)
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
                if(p.hand.size == 0){

                }
                detailedHistory.add(
                    "${p.name} played ${a.card}. Next card must be $lastSuit or $lastValue."
                )
            } else {
                // We know `a.card` is not null
                // We need to give it back to `p`.
                p.accept(a.card)
                detailedHistory.add(
                    "Returned ${a.card} to ${p.name}. Penalty of 1 card applied.")
                dealOne(p)
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
            dealOne(p)
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
            if(a?.card?.value == Value.QUEEN){
                advancePlayer(skip=true)
            }else if(a?.card?.value == Value.ACE){
                reversed = !reversed
                advancePlayer(skip=(players.size == 2))
            }else{
                advancePlayer(skip=false)
            }
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
                    + (loser?.let{listOf(it.name)} ?: listOf<String>())
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
