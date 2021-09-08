@file:Suppress("EmptyRange")

package com.sooby.cards

import com.sooby.cards.eights.Eights

@Suppress("unused", "FunctionName", "LocalVariableName")
object TestFunctions {
    private const val basicPlayers = 2
    private const val randomPlayers = 0
    private const val humanPlayers = 1
    private const val shufflePlayers = true
    private fun showAllHands(g: Eights.Game): String{
        val players = g.players + g.winners + g.losers
        return "[HANDS]\n" + players.joinToString("\n"){
            "${it.name}: ${it.showHand()}"
            }
    }
    private fun showPostgameSummary(g: Eights.Game): String =
        "\n[GAME SUMMARY]\n" +
        "[EVENT LOG]\n${g.detailedHistory.joinToString("\n")}\n" +
        "[GAME STATE]\n${g.nonPlayerSummary()}\n" +
        "[NEXT 20 PLAYERS]:\n" +
            g.predictNextPlayers(20).let{
                if(it.isNotEmpty()){
                    it.joinToString(", ")
                }else{
                    "None"
                }
            } + "\n" +
        "[PLAYERS SUMMARY]\n${g.playersSummary()}\n" +
        showAllHands(g)
    private fun buildPlayers(): List<Eights.Player> =
        (1..basicPlayers).map{
            Eights.BasicAIPlayer("Basic AI $it")
        } + (1..randomPlayers).map{
            Eights.RandomAIPlayer("Random AI $it")
        } + (1..humanPlayers).map{
            Eights.HumanPlayer("Human $it")
        }
    private fun newGame(): Eights.Game {
        val players = if(shufflePlayers){
            buildPlayers().shuffled()
        }else{
            buildPlayers()
        }
        return Eights.Game(players)
    }
    fun SetUpGame(){
        val g = newGame()
        println(showPostgameSummary(g))
    }
    fun AITurns(n: Int){
        val AI_1 = Eights.BasicAIPlayer("AI 1")
        val AI_2 = Eights.BasicAIPlayer("AI 2")
        val g = Eights.Game(listOf(AI_1, AI_2))
        try {
            (0..n).forEach { _ ->
                g.runTurn()
            }
        }catch(e: Exception){
            println(e)
        }
        println(showPostgameSummary(g))
    }
    fun CompleteGame() {
        val g = newGame().also {
            it.runAll()
        }
        println(showPostgameSummary(g))
    }
    fun TryErrors(n: Int) {
        var g = newGame()
        /*
        try {
            (0..n).forEach { _ ->
                g.runAll()
                g = newGame()
            }
            println("No errors received.")
        }catch(e: Exception){
            println(e)
            println(showPostgameSummary(g))
        }*/
        (0..n).forEach {_ ->
            g.runAll()
            g = newGame()
        }
        println("No errors received.")
    }
    fun JokerStarter() {
        for(i in (0..100)){
            val g = newGame()
            val firstCard = g.history.first()
            if(!Eights.StaticRules.wildValues.contains(firstCard.value)){
                continue
            }
            g.runAll()
            println(showPostgameSummary(g))
            break
        }
    }
}