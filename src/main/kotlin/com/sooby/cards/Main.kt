package com.sooby.cards

// import com.sooby.cards.Cards as Cards
import com.sooby.cards.eights.Eights

@Suppress("unused", "FunctionName", "LocalVariableName")
object TestFunctions {
    private const val basicPlayers = 2
    private const val randomPlayers = 2
    private const val shufflePlayers = false
    private fun showAllHands(g: Eights.Game): String{
        val players = g.players + g.winners + listOfNotNull(g.loser)
        return "[HANDS]\n" + players.joinToString("\n"){
            "${it.name}: ${it.showHand()}"
            }
    }
    private fun showPostgameSummary(g: Eights.Game): String =
        "\n[GAME SUMMARY]\n" +
        "[EVENT LOG]\n${g.detailedHistory.joinToString("\n")}\n" +
        "[GAME STATE]\n${g.nonPlayerSummary()}\n" +
        "[NEXT 20 PLAYERS]\n${g.predictNextPlayers(20)}\n" +
        "[PLAYERS SUMMARY]\n${g.playersSummary()}\n" +
        showAllHands(g)
    private fun buildPlayers(): List<Eights.Player> =
        (1..basicPlayers).map{
            Eights.BasicAIPlayer("Basic AI $it")
        } + (1..randomPlayers).map{
            Eights.RandomAIPlayer("Random AI $it")
        }
    private fun newGame(): Eights.Game{
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
            try{
                it.runAll()
            }catch(e: Exception){
                println(e)
            }
        }
        println(showPostgameSummary(g))
    }
    fun TryErrors(n: Int) {
        var error = false
        var g = newGame()
        try {
            (0..n).forEach { _ ->
                g.runAll()
                g = newGame()
            }
        }catch(e: Exception){
            println(e)
            error = true
        }
        if(!error){
            println("No errors received.")
        }else{
            println(showPostgameSummary(g))
        }
    }
}

fun main(){
    TestFunctions.CompleteGame()
}