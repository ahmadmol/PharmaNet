package com.pharmalink.data.repository

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlin.random.Random
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.merge

private const val TAG = "SupabaseRealtime"

/**
 * Subscribes to Supabase Realtime **Postgres Changes** for the given tables (schema `public`).
 *
 * **Backend setup (not enforced in app code):**
 * - Enable replication for `public.orders`, `public.requests`, `public.app_notifications` in the
 *   same Supabase publication used by Realtime (Dashboard → Database → Replication, or SQL).
 * - RLS still applies to change delivery; the client uses the logged-in user's JWT.
 * - For UPDATE/DELETE payloads that need full old rows, tables may need `REPLICA IDENTITY FULL`
 *   (optional for this app since we refresh via PostgREST on any event).
 */
@Singleton
class SupabaseRealtimeDataSource @Inject constructor(
    private val client: SupabaseClient,
) {
    /**
     * Emits a unit each time a row is inserted, updated, or deleted on [table].
     * Callers should re-query PostgREST; no row payloads are interpreted here.
     */
    fun tableChanges(table: String): Flow<Unit> = channelFlow {
        val channel = client.channel("pharma_${table}_${Random.nextInt()}")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>("public") {
            this.table = table
        }
        val updates = channel.postgresChangeFlow<PostgresAction.Update>("public") {
            this.table = table
        }
        val deletes = channel.postgresChangeFlow<PostgresAction.Delete>("public") {
            this.table = table
        }
        try {
            channel.subscribe(blockUntilSubscribed = true)
            merge(inserts, updates, deletes).collect {
                trySend(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Realtime subscription failed for public.$table", e)
        } finally {
            try {
                channel.unsubscribe()
                client.realtime.removeChannel(channel)
            } catch (e: Exception) {
                Log.e(TAG, "Realtime cleanup failed for public.$table", e)
            }
        }
    }
}
