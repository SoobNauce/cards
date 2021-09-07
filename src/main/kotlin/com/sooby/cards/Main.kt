package com.sooby.cards

// import com.sooby.cards.Cards as Cards
import com.sooby.cards.eights.Eights

@Suppress("unused", "FunctionName", "LocalVariableName")
object TestFunctions {
    private const val basicPlayers = 2
    private const val randomPlayers = 2
    private fun showAllHands(g: Eights.Game){
        val players = g.players + g.winners + listOfNotNull(g.loser)
        println("[HANDS]")
        println(players.joinToString("\n"){
            "${it.name}: ${it.showHand()}"
        })
    }
    private fun showPostgameSummary(g: Eights.Game){
        println("\n[GAME SUMMARY]")
        println("[EVENT LOG]\n${g.detailedHistory.joinToString("\n")}")
        println("[GAME STATE]\n${g.nonPlayerSummary()}")
        println("[PLAYERS SUMMARY]\n${g.playersSummary()}")
        showAllHands(g)
    }
    private fun buildPlayers(): List<Eights.Player> =
        (1..basicPlayers).map{
            Eights.BasicAIPlayer("Basic AI $it")
        } + (1..randomPlayers).map{
            Eights.RandomAIPlayer("Random AI $it")
        }
    private fun newGame(): Eights.Game{
        val players = buildPlayers()
        return Eights.Game(players)
    }
    fun SetUpGame(){
        val g = newGame()
        showPostgameSummary(g)
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
        showPostgameSummary(g)
    }
    fun CompleteGame() {
        val g = newGame().also {
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
            showPostgameSummary(g)
        }
    }
}

fun main(){
    TestFunctions.CompleteGame()
}