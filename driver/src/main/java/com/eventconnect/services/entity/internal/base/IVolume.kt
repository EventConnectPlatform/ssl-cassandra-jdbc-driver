/*
 Derived from EventConnect internal codebase and stripped down by Eray Ocak
 */

package com.eventconnect.services.entity.internal.base

import java.io.File

interface IVolume {
    operator fun get(file: String): File
}