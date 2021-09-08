@file:Suppress("MemberVisibilityCanBePrivate", "RedundantVisibilityModifier", "unused", "UNUSED_PARAMETER")

package com.sooby.cards.eights
import com.sooby.cards.Cards.*
import com.sooby.cards.CanSummarize
import com.sooby.cards.Cards.Companion as CardsCompanion

class Forfeited(message: String): Exception(message)

class Eights {
    object StaticRules {
        /** Wild values require a suit declaration */
        val wildValues = listOf(Value.EIGHT, Value.JOKER)
        /** Special values require some special handling
         * such as a suit declaration (wild),
         * reversing direction (ace),
         * or skipping the next player (queen)
        */
        // QUEEN and ACE are special values, but dissimilar enough
        // that AI needs to be smart enough to handle them without help
        // from StaticRules.
        fun fullDeck() = CardsCompanion.deckWithJokers()
        const val playersPerDeck = 6
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
        fun normalHand(): List<Card> = hand.filter{
            !StaticRules.wildValues.contains(it.value)
        }
        fun specialHand(): List<Card> = hand.filter{
            StaticRules.wildValues.contains(it.value)
        }

        /**
         * Finds the card (by exact equality) in the player's hand.
         * We don't need search-by-value because we have other ways
         * (filter) of searching hand for a card matching a description.
         */
        fun findCard(c: Card): Card? = this.hand.firstOrNull{
            it === c
        }

        fun searchAndRemove(c: Card): Card? =
            findCard(c)?.also { this.hand.remove(it) }

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
         * When the game starts, a player may be forced to pick a suit.
         * They cannot rely on game state to make this decision,
         * only the cards in their hand.
         */
        abstract fun declareFirstSuit(): Suit

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
    class BasicAIPlayer(name: String): Player(name) {
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
                return CardsCompanion.normalSuits.random()
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

        override fun declareFirstSuit() = declareSuit()

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
        }.firstOrNull{it != null}
        override fun play(
            lastCard: Card,
            lastSuit: Suit,
            lastValue: Value
        ): Attempt? {
            val c = selectCard(lastCard, lastSuit, lastValue).also {
                if(it != null){
                    require(searchAndRemove(it) != null){
                        "[$name]: A card ($it) was selected, but I couldn't" +
                        "remove it from my hand."
                    }
                }
            }
            return if(c == null){
                null
            }else if(StaticRules.wildValues.contains(c.value)){
                Attempt(c, declareSuit(lastCard, lastSuit, lastValue))
            }else{
                Attempt(c, null)
            }
        }
    }
    class RandomAIPlayer(name: String): Player(name) {
        fun mySuits(): List<Suit> =
            normalHand().map{it.suit}.distinct()
        fun myValues(): List<Value> =
            normalHand().map{it.value}.distinct()
        fun validCards(lastCard: Card, lastSuit: Suit, lastValue: Value): List<Card> =
            specialHand() + normalHand().filter{
                (it.suit == lastSuit) or (it.value == lastValue)
            }
        override fun declareSuit(): Suit =
            if(specialHand().size == handSize()){
                CardsCompanion.normalSuits.random()
            }else {
                mySuits().random()
            }
        override fun declareSuit(lastCard: Card, lastSuit: Suit, lastValue: Value): Suit =
            declareSuit()

        override fun declareFirstSuit() = declareSuit()
        override fun selectCard(lastCard: Card, lastSuit: Suit, lastValue: Value): Card? =
            validCards(lastCard, lastSuit, lastValue).randomOrNull()

        override fun play(lastCard: Card, lastSuit: Suit, lastValue: Value): Attempt? {
            val result = selectCard(lastCard, lastSuit, lastValue).also {
                // Logical || short circuits ("or" does not)
                require((it == null) || (searchAndRemove(it) != null)) {
                    "[$name]: A card ($it) was selected, but I couldn't" +
                            "remove it from my hand."
                }
            }
            return if(result == null){
                null
            }else if(StaticRules.wildValues.contains(result.value)){
                Attempt(result, declareSuit())
            }else{
                Attempt(result, null)
            }
        }
    }
    class HumanPlayer(name: String): Player(name){
        var suitToPlay: Suit? = null
        var cardToPlay: Card? = null
        var willDraw = false
        var warningsDisabled = false
        var unreadMessages: ArrayDeque<String> = ArrayDeque()
        var lastTurnSeen = -1// At the beginning, this will capture all messages.

        private fun prepareSuitString(suitString: String): String = when(suitString.uppercase()){
            "RED" -> "RED_JOKER"
            "BLACK" -> "BLACK_JOKER"
            else -> suitString.uppercase()
        }
        private fun decodeSuit(suitString: String): Suit? =
            prepareSuitString(suitString).let{u: String ->
                Suit.values().firstOrNull{
                    it.name == u
                }
            }
        private fun decodeValue(valString: String): Value? =
            valString.uppercase().let{u: String ->
                Value.values().firstOrNull{
                    it.name == u
                }
            }
        private fun gameConstraints(): Pair<Suit?, Value?> {
            val cs: List<String>? = game?.constraintsSummary()
            require(cs != null){
                "Cannot poll a nonexistent game for constraints."
            }
            var s: Suit? = null
            var v: Value? = null
            for(c in cs){
                val (ct, cd) = c.split(": ")
                when(ct){
                    "Suit" -> s = decodeSuit(cd)
                    "Value" -> v = decodeValue(cd)
                }
            }
            return s to v
        }
        private fun validateCard(c: Card): Boolean{
            if(StaticRules.wildValues.contains(c.value)){
                return true
            }
            // We will need to parse game output for this.
            // Parsing it from a string is the only way to avoid a circular dependency.
            // though we can make it easier by defining new methods
            // on the CanSummarize interface.
            val (cs, cv) = gameConstraints()
            val rejected = (
                    ((cs != null) and (c.suit != cs))
                    and ((cv != null) and (c.value != cv))
            )
            return !rejected
        }
        private fun validCards(): List<Card> =
            hand.filter {validateCard(it)}

        enum class PromptType {
            CARD,
            SUIT,
            FIRST_SUIT
        }

        /**
         * Accepted formats (case-insensitive):
         *
         * * ace of spades
         * * eight of spades (diamonds)
         * * eight of spades
         * * spades ace
         * * spades eight
         * * spades eight (diamonds)
         *
         * Note that this function discards suit declaration
         * so that will need to be detected in another function.
         */
        private fun parseSV(words: List<String>): Pair<Suit, Value>? {
            var suit: Suit? = null
            var value: Value? = null
            for(word in words) {
                when {
                    (word.contains("(")
                            or word.contains(")")) -> continue
                    word == "of" -> continue
                    else -> {
                        val ds = decodeSuit(word)
                        if (ds != null) {
                            suit = ds
                            continue
                        }
                        val dv = decodeValue(word)
                        if (dv != null) {
                            value = dv
                            continue
                        }
                    }
                }
            }
            if (suit != null) {
                if (value != null) {
                    return suit to value
                }
            }
            return null
        }
        private fun parseSuit(words: List<String>): Suit? = if(words.size == 1) {
            decodeSuit(words.first())
        }else{
            null
        }
        private fun suitAddendum(word: String): Suit? {
            val m = Regex("""\((.*)\)""").matchEntire(word)
            val g: String? = m?.groupValues?.getOrNull(1)
            return decodeSuit(g ?: "")
        }
        /**
         * This is useful for a human player in a way that it is not helpful for an AI
         * because humans can't pick cards by identity, they have to pick by suit
         * and value.
         */
        private fun findCard(s: Suit, v: Value): Card?{
            return hand.firstOrNull{
                ((it.suit == s) and (it.value == v))
            }
        }

        private fun updateUnread(){
            val gameTurn = game?.turnNumber() ?: 0
            val reducedRecents = if(lastTurnSeen == -1){
                game?.historySummary(10) ?: listOf()
            }else if(gameTurn > lastTurnSeen){
                game?.historySummary(gameTurn - lastTurnSeen)
                    ?.dropWhile{
                        !it.contains("Turn ${lastTurnSeen + 1}")
                    } ?: listOf()
            }else{
                listOf()
            }
            unreadMessages.addAll(reducedRecents)
            lastTurnSeen = gameTurn
        }

        private fun consumeUnread(): List<String> =
            unreadMessages.toList().also{
                unreadMessages.clear()
            }

        private fun promptInput(prompt: String = ""): String {
            println(prompt)
            return readLine() ?: throw java.io.EOFException("EOF while reading player input.")
        }
        private fun interactivePrompt(pt: PromptType){
            println("Unread messages:\n${consumeUnread().joinToString("\n")}")
            val (cs, cv) = gameConstraints()
            val cprompt = if(StaticRules.wildValues.contains(cv)){
                "($cs)"
            }else if(cv == null){
                ""
            }else{
                require(cs != null){
                    "Constraint value was not null, but suit was?"
                }
                "($cv or $cs)"
            }
            val prompt: String = if((pt == PromptType.FIRST_SUIT) or (pt == PromptType.SUIT)){
                    // These are cases where it does not matter what card
                    // is on the stack.
                    "Select a ${pt.toString().lowercase()}"
                }else{
                    require(cprompt != ""){
                        "Unable to build constraints prompt"
                    }
                    "Select a ${pt.toString().lowercase()}" +
                            " $cprompt " +
                            "or draw"
                }

            println("Your cards: ${showHand()}")
            println("Valid plays: ${validCards()}")
            var response = promptInput(prompt).lowercase()
            require(game != null){
                "Game must be registered before player input can be accepted."
            }
            while(response != ""){
                val words = response.split(" ")
                require(words.isNotEmpty()){
                    "Response is not empty but split returned zero segments"
                }
                val expectedName = pt.toString().lowercase()
                when(words.first()){
                    "help" -> {
                        when (words.getOrNull(1)) {
                            "show" -> println(
                                "Commands: 'show hand', 'show plays', 'show game', 'show players'.\n" +
                                "'show hand' will show your hand.\n" +
                                "'show cards' will also show your hand.\n" +
                                "'show plays' will show your hand filtered by valid plays this round.\n" +
                                "'show game' will tell you about the stack, the deck, and what cards are valid.\n" +
                                "'show players' will tell you the names of all players (including "+
                                        "players who have won the game) and their hand sizes.\n" +
                                "'show prompt' will show a reminder of what you need to select.\n" +
                                "'show history' will show game actions of the last few turns.\n\t" +
                                        "Number of turns can optionally be specified after 'show history'."
                            )
                            "disable" -> println(
                                "Validation of moves can be disabled. Players attempting to make an invalid move " +
                                "will have their move rejected and returned to them, and will be dealt a penalty card.\n" +
                                "To disable warnings, type 'disable warnings'."
                            )
                            "enable" -> println(
                                "If validation has been disabled, you can re-enable them with 'enable warnings'."
                            )
                            "exit" -> println(
                                "Immediately ends the game."
                            )
                            "debug" -> println(
                                "debug functions are: debug.constraints"
                            )
                            else -> println("Type 'help show' for instructions on using the 'show' command.\n" +
                            "Type 'help disable' for information on disabling warnings.\n" +
                            "Otherwise, select a $expectedName to continue.\n"+
                            "When playing a wild card (for example, 'eight of spades'), you can "+
                            "specify the suit in parentheses afterwards "+
                            "(such as 'eight of spades (diamonds)').")
                        }
                    }
                    "show" -> {
                        when (words.getOrNull(1)){
                            "hand" -> println("Your hand: ${showHand()}")
                            "cards" -> println("Your hand: ${showHand()}")
                            "plays" -> {
                                println("Valid plays: ${validCards()}")
                            }
                            "game" -> println(
                                game?.nonPlayerSummary() ?: "No game registered"
                            )
                            "players" -> println(
                                game?.playersSummary() ?: "No game registered"
                            )
                            "prompt" -> println(prompt)
                            "history" -> {
                                val nTurns = words.getOrNull(2)?.toInt() ?: 10
                                val h = game?.historySummary(nTurns) ?: listOf()
                                if(h.isEmpty()){
                                    println("No history to display.")
                                }else{
                                    println(h.joinToString("\n"))
                                }
                            }
                        }
                    }
                    "draw" -> {
                        willDraw = true
                        return
                    }
                    "disable" -> {
                        when(words.getOrNull(1)){
                            "warnings" -> {
                                warningsDisabled = true
                                println("Warnings have been disabled.")
                            }
                            else -> println("Only warnings can be disabled.")
                        }
                    }
                    "enable" -> {
                        when(words.getOrNull(1)){
                            "warnings" -> {
                                warningsDisabled = false
                                println("Warnings have been enabled.")
                            }
                            else -> println("Only warnings can be enabled.")
                        }
                    }
                    "debug.constraints" -> {
                        val (s,v) = gameConstraints()
                        println("Game constraints are: Suit: $s. Value: $v.")
                    }
                    "exit" -> {
                        throw Forfeited("$name quit.")
                    }
                    else -> {
                        var (s, v) = parseSV(words) ?: (null to null)
                        if(pt == PromptType.CARD){
                            if(s != null){
                                if(v != null){
                                    val c = findCard(s, v)
                                    if(c != null) {
                                        cardToPlay = c
                                        // Check for declared suit as well here
                                        s = suitAddendum(words.last())
                                        if(s != null){
                                            suitToPlay = s
                                        }
                                        willDraw = false
                                        return
                                    }else{
                                        println("Card by that description ($v of $s) is not found in hand.")
                                    }
                                }else{
                                    println("Input did not describe a valid card")
                                }
                            }else{
                                println("Input did not describe a valid card")
                            }
                        }
                        s = parseSuit(words)
                        if(pt == PromptType.SUIT){
                            if(s != null){
                                suitToPlay = s
                                willDraw = false
                                return
                            }else{
                                println("Couldn't find suit in input")
                            }
                        }
                    }
                }
                response = promptInput()
            }
        }
        //Overrides from Player

        override fun selectCard(lastCard: Card, lastSuit: Suit, lastValue: Value): Card? {
            interactivePrompt(PromptType.CARD)
            if(willDraw){ return null }
            require(cardToPlay != null){
                "Prompted for a card but couldn't find one in user input."
            }
            return cardToPlay
        }

        override fun declareSuit(): Suit {
            var s = suitToPlay
            if(s != null){
                return s
            }
            interactivePrompt(PromptType.SUIT)
            s = suitToPlay
            require(s != null){
                "Prompted for a suit but couldn't find one in user input."
            }
            return s
        }

        override fun declareSuit(lastCard: Card, lastSuit: Suit, lastValue: Value): Suit =
            declareSuit()

        override fun declareFirstSuit(): Suit {
            suitToPlay = null
            interactivePrompt(PromptType.FIRST_SUIT)
            val s = suitToPlay
            require(s != null){
                "Prompted for a first suit but couldn't find one in user input."
            }
            return s
        }

        override fun play(lastCard: Card, lastSuit: Suit, lastValue: Value): Attempt? {
            suitToPlay = null
            cardToPlay = null
            willDraw = false
            updateUnread()
            var c = selectCard(lastCard, lastSuit, lastValue)
            if(willDraw) {
                return null
            }
            while(!validCards().contains(c) and !warningsDisabled){
                    val response = promptInput("Are you sure? This does not appear to be a valid move. [Y/N]")
                    if(response == "Y"){
                        break
                    }else {
                        c = selectCard(lastCard, lastSuit, lastValue)
                    }
            }
            if(willDraw) {
                return null
            }
            require(c != null){
                "Player did not decide to draw, but card selected is still null?"
            }
            require(searchAndRemove(c) != null){
                "Couldn't remove the selected card from my hand"
            }
            return if(StaticRules.wildValues.contains(c.value)){
                val s = declareSuit()
                Attempt(c, s)
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
        var losers: MutableList<Player> = mutableListOf()
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
        fun nextSeveral(i: Int, turns: Int): List<Int> {
            if((i < 0) or (turns <= 0)){
                return listOf()
            }
            var c = i
            val result: MutableList<Int> = mutableListOf(c)
            (0..turns).forEach{ _ ->
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
        val nDecks = (players.size / StaticRules.playersPerDeck) + 1
        var deck = ArrayDeque<Card>().also{
            (1..nDecks).forEach{_ ->
                it.addAll(StaticRules.fullDeck())
            }
            it.shuffle()
        }

        /**
         * As with the deck, the convention is as follows:
         * Top (first) , ... , Bottom (last)
         * Players play cards onto the top (first). When reshuffling, all but
         * the top ("first", but most recent) card are removed.
         */
        var history: ArrayDeque<Card> = ArrayDeque()
        var turn: Int = 1
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
                if(!suppressHistory){
                    detailedHistory.add("Card dealt to ${player.name}.")
                }
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
                lastSuit = getCPlayer().declareFirstSuit()
                lastValue = firstPlay.value
                detailedHistory.add("${getCPlayer().name} chose suit for joker: $lastSuit.")
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
        fun nextValidPlayer(): Player?{
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

        fun removeLoser(p: Player){
            detailedHistory.add(
                "${p.name} has lost the game."
            )
            if(p.hand.isNotEmpty()){
                detailedHistory.add(
                    "Shuffling ${p.name}'s cards into the deck."
                )
                deck.addAll(p.hand)
                p.hand.clear()
                deck.shuffle()
            }
            val np = if(getCPlayer() == p){
                nextValidPlayer()
            }else{
                getCPlayer()
            }
            losers.add(p)
            require(players.remove(p)){
                "Couldn't remove ${p.name} from players?"
            }
            cPlayerIndex = if((np == null) || !players.contains(np)){
                detailedHistory.add("The last remaining player has lost. The game is over.")
                -1
            }else{
                players.indexOf(np)
            }
        }

        /**
         * Returns true if the current player was removed,
         * which either means the turn was advanced or there are no players left.
         */
        fun removeWinners(): Boolean{
            if(players.isEmpty()){
                return false
            }
            // np should be a player who is not in danger of being removed here.
            val cp = getCPlayer()
            val np = nextValidPlayer()
            var advance = false
            // Find winners first
            // The call to toList() is because we can't alter players.filter
            // (which is backed by players) while iterating over it.
            fun winners() = players.filter{it.handSize() == 0}.toList()
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
                if(p == cp){
                    advance = true
                }
            }
            when(players.size) {
                0 -> {
                    detailedHistory.add(
                        "All players have won. The game will end."
                    )
                    cPlayerIndex = -1
                    // The current player must have been removed for there to be zero players.
                    return true
                }
                1 -> {
                    val p = players.first()
                    // "last remaining player has lost" is already
                    // added to history in removeLoser
                    removeLoser(p)
                    // There can only be the "current player" here, so return true.
                    return true
                }
                else -> {
                    require(players.contains(np)){
                        "nextValidPlayer returned the identity of a player who no longer exists."
                    }
                    val pi = players.indexOf(np)
                    require(!advance || (pi > -1)){
                        "Next valid player was not found despite the player having existed " +
                        "for the purpose of contains()."
                    }
                    cPlayerIndex = if(advance){
                        players.indexOf(np)
                    }else{
                        cPlayerIndex
                    }
                    return advance
                }
            }
        }

        /**
         * Update state to reflect the next player.
         */
        fun advancePlayer(skip: Boolean = false){
            val alreadyAdvanced = removeWinners()
            // Now that winners have been removed, advance to the next player.
            // if players is size 0, we already handled this in removeWinners.
            // players should not be size 1, because that would mean
            // the single loser was not removed in removeWinners.
            require(players.size != 1){
                "Last remaining player was not removed by removeWinners"
            }
            if(players.size > 1){
                if(!alreadyAdvanced){
                    cPlayerIndex = if(skip) {
                        nextFromIndex(nextFromIndex(cPlayerIndex))
                    }else{
                        nextFromIndex(cPlayerIndex)
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
                when(p.handSize()){
                    0 -> {detailedHistory.add(
                        "${p.name} has zero cards and will be declared a winner."
                    )}
                    1 -> {detailedHistory.add(
                        "${p.name} has one card remaining."
                    )}
                }
            } else {
                // We know `a.card` is not null
                // We need to give it back to `p`.
                p.accept(a.card)
                detailedHistory.add(
                    "${p.name} played an invalid card (${a.card}). " +
                        "This has been returned to them with a penalty of 1 card.")
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
                "${p.name} has chosen to draw."
            )
            // We don't need to worry about infinite loops.
            // If a player tries to draw and there aren't enough cards
            // even after reshuffling, they simply lose their turn.
            safeDeal(p)
        }

        fun runTurn(){
            detailedHistory.add("Turn $turn began.")
            require(players.isNotEmpty()){
                "All players have left the game."
            }
            if(players.size == 1){
                removeLoser(players.first())
                return
            }
            val p = getCPlayer()
            require(p.handSize() > 0){
                "${p.name} has a hand size of zero but has not already won?"
            }
            var skipped = false
            val a: Attempt? = try {
                p.play(
                    history.first(),
                    lastSuit,
                    lastValue
                )
            }catch(e: Forfeited){
                removeLoser(p)
                skipped = true
                null
            }
            // if skipped is true, then the turn has already been advanced.
            if(!skipped) {
                if (a != null) {
                    // It's in advancePlayer() that winners are detected.
                    acceptAttempt(a, p)
                } else {
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
                advancePlayer(skip)
            }
            turn += 1
        }
        fun runAll(){
            while(players.size > 1){
                runTurn()
            }
            if(players.size == 1){
                val p = players.first()
                removeLoser(p)
            }
        }
        // Summary functions
        override fun playersSummary() =
            listOf(
                players.joinToString("\n") {
                "${it.name} (player) (${it.handSize()} cards)"},
                winners.joinToString("\n"){
                "${it.name} (winner) (${it.handSize()} cards)"},
                losers.joinToString("\n"){
                "${it.name} (loser) (${it.handSize()} cards)"}
            ).joinToString("\n")

        override fun playerNames(): List<String> =
            (players.map{it.name}
                    + winners.map{it.name}
                    + losers.map{it.name}
            )

        override fun historySummary(turns: Int): List<String> =
            if(turns > turn){
                detailedHistory.toList()
            }else{
                detailedHistory.dropWhile{
                    !it.contains("Turn ${turn - turns}")
                }
            }

        override fun decisionSummary(turns: Int) = historySummary(turns).filter{
            (("played" in it)
            or ("chosen" in it)
            )
        }

        override fun turnNumber() = turn

        override fun nonPlayerSummary() = """Stack (${history.size} cards): Top card is ${history.first()}.
            |Next card must match $lastValue or $lastSuit.
            |${deck.size} cards in deck.""".trimMargin("|")

        override fun predictNextPlayers(turns: Int): List<String> = when (players.size) {
            0 -> {
                listOf()
            }
            1 -> {
                listOf(players.first().name)
            }
            else -> {
                nextSeveral(cPlayerIndex, turns).map {
                    players[it].name
                }
            }
        }

        override fun predictNextPlayer() = predictNextPlayers(1).let {
            if(it.isEmpty()){
                "N/A"
            }else{
                it.first()
            }
        }

        override fun constraintsSummary(): List<String> =
            listOf("Suit: $lastSuit", "Value: $lastValue")
    }
}
