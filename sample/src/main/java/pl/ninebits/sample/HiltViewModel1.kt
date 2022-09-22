package pl.ninebits.sample

import pl.ninebits.ksp.annotation.AssistedAutogenFactory
import pl.ninebits.mock.ViewModel
import java.util.UUID


class HiltViewModel1(
    val injectedByHilt: UUID,
    val userInputId: Int,
    val testRandom: Any?,
) : ViewModel()

@AssistedAutogenFactory
interface HiltViewModel1Factory {
    fun create(userInputId: Int, testRandom: Any?): HiltViewModel1
}