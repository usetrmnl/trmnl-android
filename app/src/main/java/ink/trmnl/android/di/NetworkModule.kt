package ink.trmnl.android.di

import android.content.Context
import com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory
import com.slack.eithernet.integration.retrofit.ApiResultConverterFactory
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.optional.SingleIn
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import ink.trmnl.android.data.AppConfig.TRMNL_API_SERVER_BASE_URL
import ink.trmnl.android.network.TrmnlApiService
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

@Module
@ContributesTo(AppScope::class)
object NetworkModule {
    // Cache size for OkHttp (10 MB)
    private const val CACHE_SIZE = 10 * 1024 * 1024L

    @Provides
    @SingleIn(AppScope::class)
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
    ): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

        // Create cache directory
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, CACHE_SIZE)

        // Create custom user agent with app info and version
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appName = context.getString(context.applicationInfo.labelRes)
        val versionName = packageInfo.versionName
        val versionCode = packageInfo.longVersionCode

        val userAgent = "$appName/$versionName (build:$versionCode; Android ${android.os.Build.VERSION.RELEASE})"

        return OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val request =
                    chain
                        .request()
                        .newBuilder()
                        .header("User-Agent", userAgent)
                        .build()
                chain.proceed(request)
            }.addInterceptor(loggingInterceptor)
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideMoshi(): Moshi =
        Moshi
            .Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    @Provides
    @SingleIn(AppScope::class)
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(TRMNL_API_SERVER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ApiResultConverterFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ApiResultCallAdapterFactory)
            .build()

    @Provides
    @SingleIn(AppScope::class)
    fun provideTrmnlApiService(retrofit: Retrofit): TrmnlApiService = retrofit.create(TrmnlApiService::class.java)
}
