package pl.ninebits.ksp.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AssistedAutogenFactory(val methodName: String = "create")
