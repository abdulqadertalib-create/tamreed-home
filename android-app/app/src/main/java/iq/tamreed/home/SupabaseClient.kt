package iq.tamreed.home

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {

    private const val SUPABASE_URL =
        "https://pmjmfeymnahpockjjafn.supabase.co"

    private const val SUPABASE_KEY =
        "sb_publishable_HtMExFgxiFq_qhN2I9V76w_0YFG6L0j"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}
