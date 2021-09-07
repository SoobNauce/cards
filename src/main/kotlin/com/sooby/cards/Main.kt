package com.sooby.cards

import com.sooby.cards.Cards as Cards
import com.sooby.cards.eights.Eights

object TestFunctions {
    val playerCount = 50
    fun showAllHands(g: Eights.Game){
        val players = g.players
        println("[HANDS]")
        println(players.joinToString("\n"){
            "${it.name}: ${it.showHand()}"
        })
    }
    fun showPostgameSummary(g: Eights.Game){
        println("\n[GAME SUMMARY]")
        println("[EVENT LOG]\n${g.detailedHistory.joinToString("\n")}")
        println("[GAME STATE]\n${g.nonPlayerSummary()}")
        println("[PLAYERS SUMMARY]\n${g.playersSummary()}")
        showAllHands(g)
    }
    fun newGame(nPlayers: Int): Eights.Game{
        val players = (1..nPlayers).map{Eights.BasicAIPlayer("AI $it")}
        return Eights.Game(players)
    }
    fun SetUpGame(): Unit {
        val g = newGame(playerCount)
        showPostgameSummary(g)
    }
    fun AITurns(n: Int): Unit {
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
        showPostgameSummary(g)
    }
    fun CompleteGame() {
        val g = newGame(playerCount).also {
            try{
                it.runAll()
            }catch(e: Exception){
                println(e)
            }
        }
        showPostgameSummary(g)
    }
    fun FishForErrors(n: Int) {
        var error = false
        var g = newGame(playerCount)
        try {
            (0..n).forEach { _ ->
                g.runAll()
                g = newGame(playerCount)
            }
        }catch(e: Exception){
            println(e)
            error = true
        }
        if(!error){
            println("No errors received.")
        }else{
            showPostgameSummary(g)
        }
    }
}

fun main(){
    TestFunctions.CompleteGame()
}