package com.sooby.cards.eights

const val DEFAULT_HUMANS = 1
const val DEFAULT_BASICS = 1
const val DEFAULT_RANDOMS = 0

fun main(args: Array<String>){
    // If specified, the first three args represent number of players of each type
    // first: Human players
    // second: Basic AI
    // third: Random AI
    val humans = args.elementAtOrNull(0)?.toIntOrNull() ?: DEFAULT_HUMANS
    val basics = args.elementAtOrNull(1)?.toIntOrNull() ?: DEFAULT_BASICS
    val randoms = args.elementAtOrNull(2)?.toIntOrNull() ?: DEFAULT_RANDOMS
    println("Type 'help' for help.")
    val humanNames: List<String?> = (1..humans).map {
        println("Human $it: Enter name")
        readLine()?.trim()
    }
    require(!humanNames.contains(null)){
        "Not enough names were given to start the game."
    }
    val players: List<Eights.Player> = humanNames.mapIndexed{ i: Int, it: String? ->
        Eights.HumanPlayer(it ?: "Human $i")
    } + (1..basics).map{Eights.BasicAIPlayer("Basic AI $it")
    } + (1..randoms).map{Eights.RandomAIPlayer("Random AI $it")}
    val g = Eights.Game(players)
    g.runAll()
    println("The game is over. Summary of last few turns:")
    println(g.historySummary(10).joinToString("\n"))
}