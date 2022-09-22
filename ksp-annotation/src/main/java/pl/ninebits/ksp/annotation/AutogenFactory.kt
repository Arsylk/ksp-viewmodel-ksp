package pl.ninebits.ksp.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AutogenFactory(val suffix: String = "Factory")
