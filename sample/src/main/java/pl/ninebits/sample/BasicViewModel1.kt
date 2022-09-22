package pl.ninebits.sample

import pl.ninebits.ksp.annotation.AutogenFactory
import pl.ninebits.mock.ViewModel

@AutogenFactory
class BasicViewModel1(
    val int: Int,
    val int1: Int,
    val string: String?,
    val any: Any?,
) : ViewModel()