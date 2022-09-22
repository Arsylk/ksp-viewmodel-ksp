package pl.ninebits.mock


inline fun <reified VM: ViewModel> viewModels(crossinline factory: () -> ViewModelProvider.Factory): Lazy<VM> = lazy {
    error("Mock implementation")
}