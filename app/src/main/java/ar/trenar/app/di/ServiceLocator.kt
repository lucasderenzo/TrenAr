package ar.trenar.app.di

import android.content.Context
import ar.trenar.app.data.TrenRepository
import ar.trenar.app.data.local.PrefsStore
import ar.trenar.app.data.local.StationCatalog
import ar.trenar.app.data.remote.AuthInterceptor
import ar.trenar.app.data.remote.SofseApi
import ar.trenar.app.data.remote.SofseAuthenticator
import ar.trenar.app.data.remote.TokenStore
import ar.trenar.app.location.LocationHelper
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Minimal manual dependency container — no annotation processing. */
object ServiceLocator {

    private const val BASE_URL = "https://api-servicios.sofse.gob.ar/v1/"

    lateinit var repository: TrenRepository
        private set
    lateinit var prefs: PrefsStore
        private set
    lateinit var location: LocationHelper
        private set
    lateinit var tokenStore: TokenStore
        private set
    lateinit var appContext: Context
        private set

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val app = context.applicationContext
            appContext = app

            tokenStore = TokenStore(app)
            val authenticator = SofseAuthenticator(BASE_URL.trimEnd('/'), tokenStore)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(authenticator))
                .addInterceptor(logging)
                .build()

            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            }
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            val api = retrofit.create(SofseApi::class.java)

            val catalog = StationCatalog(app)
            repository = TrenRepository(api, catalog)
            prefs = PrefsStore(app)
            location = LocationHelper(app)
            initialized = true
        }
    }
}
