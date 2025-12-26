package com.focusroom.muse.muse

import com.focusroom.muse.model.RawEegSample
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the raw Muse EEG stream. The app ships with a LibMuse implementation,
 * but you can swap in a mock generator during UI development or testing.
 */
interface MuseDataSource {
    fun openStream(): Flow<RawEegSample>
}
