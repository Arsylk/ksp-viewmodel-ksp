package pl.ninebits

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pl.ninebits.mock.viewModels
import pl.ninebits.sample.*
import java.util.*


fun main(args: Array<String>) {
//    val viewModel1 by viewModels<BasicViewModel1> {
//        BasicViewModel1Factory(1, 2, "te", Random())
//    }
//    lateinit var assisted1: HiltViewModel1Factory
//    val hiltViewModel1 by viewModels<HiltViewModel1> {
//        assisted1(testRandom = null, userInputId = 132)
//    }
    val s1 = MutableStateFlow("query")
    val s2 = MutableStateFlow(1)

    val new = combine(s1, s2) { t1, t2 -> t1 to t2 }
        .shareIn(GlobalScope, SharingStarted.Eagerly)

    GlobalScope.launch {
        delay(2000)
        new.collect {
            print("first: $it")
        }
    }
    GlobalScope.launch {
        delay(1000)
        new.collect {
            print("second: $it")
        }
    }
    runBlocking { delay(5000) }
}