/*
 Derived from EventConnect internal codebase and stripped down by Eray Ocak
 */

package com.eventconnect.services.util.env

import com.eventconnect.services.env

object Env {
    operator fun get(key: String): String? {
        return env[key]
    }
}