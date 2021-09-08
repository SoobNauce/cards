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
     * Summarize recent actions of the last few turns.
     * Not guaranteed to be able to return full history of the game,
     * only what is convenient for the game to remember.
     */
    fun historySummary(turns: Int): List<String>

    /**
     * Summarize all decisions made by each player.
     * An alternative to historySummary if the player is good enough
     * at reconstructing game state.
     */
    fun decisionSummary(turns: Int): List<String>

    /**
     * Returns the current turn number.
     * This should always be increasing but multiple actions
     * can be taken on the same turn.
     */

    fun turnNumber(): Int

    /**
     * Summarize all instantaneous information about the game,
     * such as the top most card on the stack. Leaves out information about
     * other players. (e.x. go fish might return "No non-player state")
     */
    fun nonPlayerSummary(): String

    /**
     * Summarize restrictions on possible plays
     * for the current turn.
     * This should be machine-readable.
     * An empty list means any move is valid.
     */
    fun constraintsSummary(): List<String>

    /**
     * Names the player who will play after the current player.
     */
    fun predictNextPlayer(): String

    /**
     * Lists players in order of who is expected to play next, up to
     * a certain depth. Does not predict changes to turn order.
     */
    fun predictNextPlayers(turns: Int): List<String>

}