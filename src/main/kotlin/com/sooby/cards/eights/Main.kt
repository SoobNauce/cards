package com.sooby.cards.eights

import kotlin.system.exitProcess

const val DEFAULT_BASICS = 1
const val DEFAULT_RANDOMS = 0
const val DEFAULT_HUMANS = 1
const val HELP_STRING = (
"""Specify number of basic AI players, random AI players, and human players (default $DEFAULT_BASICS / $DEFAULT_RANDOMS / $DEFAULT_HUMANS).
All three are optional and specified in that order.
There is no special handling for multiple human players, but you can specify zero if you want to view an AI vs AI game.
Players can also be specified using the following flags: --BasicAIs=0, --RandomAIs=0, --HumanPlayers=0.
If number of players is specified via -- flags, then there are no positional arguments accepted.
Type "help" in-game for a summary of possible actions.
"""
)
val HELP_FLAGS = listOf("-?", "help", "-h")

@Suppress("SpellCheckingInspection")
fun unpackArgs(args: Array<String>): Triple<Int, Int, Int>{
    fun removeHyphens(s: String): String? {
        return if(s.slice(0..1) != "--"){
            null
        }else{
            s.drop(2)
        }
    }
    var (basics, randoms, humans) = Triple(DEFAULT_BASICS, DEFAULT_RANDOMS, DEFAULT_HUMANS)
    if(args.any {
        (it.contains("--")
        or (HELP_FLAGS.contains(it))
    )}){
        for(sString in args){
            val isInt = sString.toIntOrNull() != null
            when{
                // flag passed was --
                sString == "--" -> {
                    println("There is nothing to specify with '--'")
                    exitProcess(1)
                }
                listOf("-?", "help", "--help", "-h").contains(sString) -> {
                    println(HELP_STRING)
                    exitProcess(0)
                }
                isInt -> {
                    println("When flags are specified, positional arguments are not supported.")
                    exitProcess(1)
                }
            }
            val flag = removeHyphens(sString)?.lowercase()?.split("=") ?: listOf()
            if(flag.size != 2) {
                println("Unknown flag: $sString")
                exitProcess(1)
            }
            val n = try {
                flag[1].toInt()
            }catch(e: NumberFormatException){
                println("Couldn't get int value from flag: $sString")
                exitProcess(1)
            }
            when(flag.first()){
                "basicais" -> basics = n
                "randomais" -> randoms = n
                "humanplayers" -> humans = n
            }
        }
        return Triple(basics, randoms, humans)
    }else{
        val basicsString = args.elementAtOrNull(0)
        val randomsString = args.elementAtOrNull(1)
        val humansString = args.elementAtOrNull(2)
        return when{
            basicsString == null -> {
                Triple(basics, randoms, humans)
            }
            randomsString == null -> {
                Triple(basicsString.toInt(), randoms, humans)
            }
            humansString == null -> {
                Triple(basicsString.toInt(), randomsString.toInt(), humans)
            }
            (args.size > 3) -> {
                println("Too many args to set up players.")
                exitProcess(1)
            }
            else -> {
                Triple(basicsString.toInt(), randomsString.toInt(), humansString.toInt())
            }
        }
    }
}

fun main(args: Array<String>){
    // If specified, the first three args represent number of players of each type
    // first: Human players
    // second: Basic AI
    // third: Random AI
    val (basics, randoms, humans) = unpackArgs(args)
    if((basics + randoms + humans) < 2){
        println("Not enough players specified to start a game.")
        exitProcess(1)
    }
    println("Type 'help' for help.")
    val humanNames: List<String?> = (1..humans).map {
        println("Human $it: Enter name")
        readLine()?.trim()
    }
    require(!humanNames.contains(null)){
        "Not enough names were given to start the game."
    }
    val players: List<Eights.Player> =
        (1..basics).map{Eights.BasicAIPlayer("Basic AI $it")} +
        (1..randoms).map{Eights.RandomAIPlayer("Random AI $it")} +
        humanNames.mapIndexed{ i: Int, it: String? ->
            Eights.HumanPlayer(it ?: "Human $i")}
    val g = Eights.Game(players)
    g.runAll()
    println("\n" + g.postgameSummary())
}