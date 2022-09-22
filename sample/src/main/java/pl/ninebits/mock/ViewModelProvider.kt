package pl.ninebits.mock

class ViewModelProvider {
    public interface Factory {
        public fun <T : ViewModel> create(modelClass: Class<T>): T {
            throw UnsupportedOperationException(
                "Factory.create(String) is unsupported.  This Factory requires " +
                        "`CreationExtras` to be passed into `create` method."
            )
        }
    }
}