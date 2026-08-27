package iq.tamreed.home

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://pmjmfeymnahpockjjafn.supabase.co",
        supabaseKey = "sb_publishable_HtMExFgxiFq_qhN2I9V76w_0YFG6L0j"
    ) {
        install(Auth)
        install(Postgrest)
    }
}
