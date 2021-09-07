package com.sooby.cards

interface CanSummarize {
    /**
     * List players including all public information about them
     * (such as hand size)
     */
    fun playersSummary(): String

    /**
     * List players by name. Each player should have a unique name.
     * The order of this should not change if turn order changes.
     */
    fun playerNames(): List<String>

    /**
     * Summarize recent actions taken.
     * Not guaranteed to return full history of the game,
     * only what is convenient for the game to remember.
     */
    fun historySummary(depth: Int): List<String>

    /**
     * Summarize all instantaneous information about the game,
     * such as the top most card on the stack. Leaves out information about
     * other players. (e.x. go fish might return "No non-player state")
     */
    fun nonPlayerSummary(): String

    /**
     * Names the player who will play after the current player.
     */
    fun predictNextPlayer(): String

    /**
     * Lists players in order of who is expected to play next, up to
     * a certain depth. Does not predict changes to turn order.
     */
    fun predictNextPlayers(depth: Int): List<String>
}