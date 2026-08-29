package iq.tamreed.home

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {

    private const val SUPABASE_URL =
        "https://vpnqznfwnqlsztwlcepr.supabase.co"

    private const val SUPABASE_KEY =
        "sb_publishable_Zj_mqQ7rVoD_lAqiPR7BVw_MnxzURwZ"

    val client: SupabaseClient =
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {

            install(Auth)

            install(Postgrest)
        }
}
