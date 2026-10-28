package ke.tahadhari.data

import ke.tahadhari.data.remote.FloodDaily
import ke.tahadhari.data.remote.MAX_POINTS_PER_CALL
import ke.tahadhari.data.remote.ForecastApi
import ke.tahadhari.data.remote.RainfallDaily
import ke.tahadhari.domain.CountyForecast
import ke.tahadhari.domain.Dates
import ke.tahadhari.domain.DailyRain
import ke.tahadhari.domain.Freshness
import ke.tahadhari.domain.RiverOutlook
import ke.tahadhari.domain.UpstreamForecast
import ke.tahadhari.domain.UserPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * A forecast plus where it came from.
 * [fromCache] is true when the network failed and we fell back to the last saved fetch;
 * [fetchedAtMillis] is when the (rainfall) data was actually downloaded.
 */
data class ForecastResult(
    val forecast: CountyForecast,
    val fetchedAtMillis: Long,
    val fromCache: Boolean,
)

/**
 * Fetches and combines rainfall + river data for a county, with an offline cache.
 *
 * - Success: returns fresh data and saves the raw API data to [cache].
 * - Rainfall fetch fails: returns the cached data (marked [ForecastResult.fromCache]); days that
 *   are already in the past are dropped. If there is no usable cache, the original error is rethrown.
 * - Only the river fetch fails: uses the cached river data if it is recent enough, so a flaky
 *   flood API doesn't silently remove the river signal.
 *
 * Deciding whether cached data is too old to show at all is the caller's job (see [Freshness]).
 */
class ForecastRepository(
    private val api: ForecastApi,
    private val cache: ForecastCache,
    /**
     * True when the upstream-rain signal is on (`RiskThresholds.upstreamRainFloor != null`). Only then are upstream
     * counties' rain fetched: one extra batched call in [forecastFor] (at most), none in [forecastsFor] (it reuses the batch).
     */
    private val upstreamEnabled: Boolean = false,
    private val nowMillis: () -> Long,
) {

    suspend fun forecastFor(county: County): ForecastResult {
        val fetched = try {
            fetch(county)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return fromCache(county) ?: throw e
        }
        cache.write(fetched)
        val forecast = buildForecast(county, fetched.rainfall, fetched.flood, today = null, soilMoisture = fetched.soilMoistureM3m3,
            upstream = fetched.upstream, expectsUpstream = expectsUpstream(county))
        return ForecastResult(forecast, fetched.fetchedAtMillis, fromCache = false)
    }

    private fun expectsUpstream(county: County) = upstreamEnabled && county.upstreamCodes.isNotEmpty()

    /** One batched call for the upstream counties' rain. Null on any failure: local data is still valid without it. */
    private suspend fun fetchUpstream(county: County): List<CachedUpstream>? {
        val upstream = county.upstreamCodes.mapNotNull { Counties.byCode(it) }
        if (upstream.isEmpty()) return null
        return try {
            val responses = api.dailyRainfallBatch(upstream.map { it.location })
            check(responses.size == upstream.size) { "Expected ${upstream.size} upstream results but got ${responses.size}" }
            val at = nowMillis()
            upstream.zip(responses).mapNotNull { (c, response) ->
                response?.daily?.takeIf { it.time.isNotEmpty() }?.let { CachedUpstream(c.code, at, it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetch(county: County): CachedForecast = coroutineScope {
        val rainCall = async { api.dailyRainfall(queryPoint(county)) }
        val floodCall = county.riverPoint?.let { point ->
            async {
                try {
                    api.riverDischarge(point).daily
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            }
        }
        val upstreamCall = if (expectsUpstream(county)) async { fetchUpstream(county) } else null
        val rain = rainCall.await()
        assemble(county, rain.daily, rain.current?.soilMoisture, floodCall?.await(), expectsFlood = floodCall != null, now = nowMillis(),
            freshUpstream = upstreamCall?.await())
    }

    /**
     * Builds what we save for one county from fresh rainfall and (maybe) fresh river data.
     * If the county has a river but the river data is missing, reuse recent cached river data
     * so a flaky flood API doesn't silently remove the river signal.
     */
    private suspend fun assemble(
        county: County,
        rain: RainfallDaily,
        soilMoisture: Double?,
        freshFlood: FloodDaily?,
        expectsFlood: Boolean,
        now: Long,
        freshUpstream: List<CachedUpstream>? = null,
    ): CachedForecast {
        var flood = freshFlood
        var floodAt: Long? = if (flood != null) now else null
        if (expectsFlood && flood == null) {
            cache.read(county.code, ForecastCache.pointOf(county))?.takeIf { cached ->
                val at = cached.floodFetchedAtMillis
                cached.flood != null && at != null && !Freshness.isTooOldToShow(at, now)
            }?.let { flood = it.flood; floodAt = it.floodFetchedAtMillis }
        }
        // Upstream rain: fresh data first; any upstream county missing from it is filled from recent cached data.
        val upstream = if (!expectsUpstream(county)) emptyList() else {
            val fresh = freshUpstream.orEmpty()
            val have = fresh.map { it.countyCode }.toSet()
            val cachedUp = if (county.upstreamCodes.any { it !in have }) {
                cache.read(county.code, ForecastCache.pointOf(county))?.upstream.orEmpty()
                    .filter { it.countyCode in county.upstreamCodes && it.countyCode !in have && !Freshness.isTooOldToShow(it.fetchedAtMillis, now) }
            } else emptyList()
            fresh + cachedUp
        }
        return CachedForecast(
            countyCode = county.code,
            fetchedAtMillis = now,
            rainfall = rain,
            soilMoistureM3m3 = soilMoisture,
            upstream = upstream,
            flood = flood,
            floodFetchedAtMillis = floodAt,
            pointLatitude = ForecastCache.pointOf(county)?.latitude,
            pointLongitude = ForecastCache.pointOf(county)?.longitude,
        )
    }

    /**
     * Forecasts for many counties using batched API calls (one rainfall call and one river call
     * for up to [MAX_POINTS_PER_CALL] points each) instead of one call per county.
     *
     * Per county the cache rules are the same as [forecastFor]: success is saved; if that county's
     * data is missing, or its whole batch call failed, the saved forecast is served (marked
     * [ForecastResult.fromCache], past days dropped); with no usable cache the county's entry is a failure.
     * A failed river batch only costs the river signal (recent cached river data is reused).
     * The result has one entry per county code in [counties].
     */
    suspend fun forecastsFor(counties: List<County>): Map<Int, Result<ForecastResult>> = coroutineScope {
        val rainByCode = HashMap<Int, RainfallDaily>()
        val soilByCode = HashMap<Int, Double>()
        val rainErrors = HashMap<Int, Throwable>()
        val floodByCode = HashMap<Int, FloodDaily>()

        val rainJobs = counties.chunked(MAX_POINTS_PER_CALL).map { chunk ->
            async {
                try {
                    val responses = api.dailyRainfallBatch(chunk.map { queryPoint(it) })
                    check(responses.size == chunk.size) { "Expected ${chunk.size} rainfall results but got ${responses.size}" }
                    chunk.zip(responses).forEach { (county, response) ->
                        val daily = response?.daily
                        if (daily != null && daily.time.isNotEmpty()) {
                            rainByCode[county.code] = daily
                            response?.current?.soilMoisture?.let { soilByCode[county.code] = it }
                        }
                        else rainErrors[county.code] = RuntimeException("No rainfall data for ${county.name}")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    chunk.forEach { rainErrors[it.code] = e }
                }
            }
        }
        val riverCounties = counties.filter { it.riverPoint != null }
        val floodJobs = riverCounties.chunked(MAX_POINTS_PER_CALL).map { chunk ->
            async {
                try {
                    val responses = api.riverDischargeBatch(chunk.map { it.riverPoint!! })
                    check(responses.size == chunk.size) { "Expected ${chunk.size} river results but got ${responses.size}" }
                    chunk.zip(responses).forEach { (county, response) ->
                        response?.daily?.let { floodByCode[county.code] = it }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // River data is optional; assemble() falls back to recent cached river data.
                }
            }
        }
        (rainJobs + floodJobs).forEach { it.await() }

        val now = nowMillis()
        counties.associate { county ->
            val rain = rainByCode[county.code]
            county.code to if (rain != null) {
                try {
                    // Upstream rain comes from the same batch (the dashboard loads all 47): no extra HTTP call.
                    val upstreamFromBatch = if (expectsUpstream(county)) county.upstreamCodes.mapNotNull { code ->
                        rainByCode[code]?.let { CachedUpstream(code, now, it) }
                    } else null
                    val fetched = assemble(county, rain, soilByCode[county.code], floodByCode[county.code], county.riverPoint != null, now, upstreamFromBatch)
                    cache.write(fetched)
                    val forecast = buildForecast(county, fetched.rainfall, fetched.flood, today = null, soilMoisture = fetched.soilMoistureM3m3,
                        upstream = fetched.upstream, expectsUpstream = expectsUpstream(county))
                    Result.success(ForecastResult(forecast, fetched.fetchedAtMillis, fromCache = false))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    fromCache(county)?.let { Result.success(it) } ?: Result.failure(e)
                }
            } else {
                val cached = fromCache(county)
                if (cached != null) Result.success(cached)
                else Result.failure(rainErrors[county.code] ?: RuntimeException("No data for ${county.name}"))
            }
        }
    }

    private suspend fun fromCache(county: County): ForecastResult? {
        val cached = cache.read(county.code, ForecastCache.pointOf(county)) ?: return null
        val today = Dates.isoDateFromEpochMillis(nowMillis())
        val forecast = buildForecast(county, cached.rainfall, cached.flood, today, cached.soilMoistureM3m3,
            upstream = cached.upstream, expectsUpstream = expectsUpstream(county))
        if (forecast.days.isEmpty()) return null // every cached day is already in the past
        return ForecastResult(
            forecast.copy(fetchedAtDate = Dates.isoDateFromEpochMillis(cached.fetchedAtMillis)),
            cached.fetchedAtMillis,
            fromCache = true,
        )
    }

    companion object {
        /** Where rainfall is requested: the county HQ, or the user's point rounded to ~1 km (privacy). */
        private fun queryPoint(county: County): GeoPoint =
            if (ForecastCache.pointOf(county) != null) UserPoint.round(county.location) else county.location

        /**
         * Rebuilds a [CountyForecast] from raw API data. When [today] is given (cached data),
         * days before it are dropped because they are no longer a forecast.
         */
        fun buildForecast(
            county: County,
            rain: RainfallDaily,
            flood: FloodDaily?,
            today: String?,
            soilMoisture: Double? = null,
            upstream: List<CachedUpstream> = emptyList(),
            expectsUpstream: Boolean = false,
        ): CountyForecast {
            val days = dailyRains(rain, today)
            val firstDay = today ?: days.firstOrNull()?.date ?: ""

            val upstreamForecasts = if (!expectsUpstream) emptyList() else upstream.mapNotNull { u ->
                val name = Counties.byCode(u.countyCode)?.name ?: return@mapNotNull null
                dailyRains(u.rainfall, today).takeIf { it.isNotEmpty() }?.let { UpstreamForecast(name, it) }
            }
            val river = flood?.let {
                RiverOutlook.fromSeries(county.riverName, it.time, it.riverDischarge, firstDay)
            }
            return CountyForecast(
                county = county, days = days, river = river, fetchedAtDate = firstDay,
                soilMoistureM3m3 = soilMoisture,
                upstream = upstreamForecasts,
                // Any upstream county missing (or all its days already past) means "not fully checked", never "dry".
                upstreamUnavailable = expectsUpstream && upstreamForecasts.size < county.upstreamCodes.size,
            )
        }

        private fun dailyRains(rain: RainfallDaily, today: String?): List<DailyRain> = rain.time.mapIndexed { i, date ->
            DailyRain(
                date = date,
                precipitationMm = rain.precipitationSum.getOrNull(i) ?: 0.0,
                probabilityPercent = rain.precipitationProbabilityMax.getOrNull(i),
            )
        }.filter { today == null || it.date >= today }
    }
}
