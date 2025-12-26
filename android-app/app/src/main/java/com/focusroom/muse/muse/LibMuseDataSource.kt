@file:Suppress("MissingPermission")

package com.focusroom.muse.muse

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.choosemuse.libmuse.Eeg
import com.choosemuse.libmuse.Muse
import com.choosemuse.libmuse.MuseConnectionListener
import com.choosemuse.libmuse.MuseConnectionPacket
import com.choosemuse.libmuse.MuseDataListener
import com.choosemuse.libmuse.MuseDataPacket
import com.choosemuse.libmuse.MuseDataPacketType
import com.choosemuse.libmuse.MuseManagerAndroid
import com.focusroom.muse.model.Channel
import com.focusroom.muse.model.RawEegSample
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Streams raw EEG packets from the first available Muse headset using the official LibMuse SDK.
 */
class LibMuseDataSource(
    private val context: Context,
    private val preferredMac: String? = null,
) : MuseDataSource {

    private val manager: MuseManagerAndroid = MuseManagerAndroid.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun openStream(): Flow<RawEegSample> = callbackFlow {
        manager.setContext(context)
        manager.stopListening()
        manager.startListening()

        val isClosed = AtomicBoolean(false)
        val dataListener = object : MuseDataListener() {
            override fun receiveMuseDataPacket(packet: MuseDataPacket?, muse: Muse?) {
                if (packet == null || muse == null || packet.packetType != MuseDataPacketType.EEG) return
                val sample = RawEegSample(
                    timestampMillis = System.currentTimeMillis(),
                    channelsMicrovolts = mapOf(
                        Channel.TP9 to packet.getEegChannelValue(Eeg.EEG1),
                        Channel.AF7 to packet.getEegChannelValue(Eeg.EEG2),
                        Channel.AF8 to packet.getEegChannelValue(Eeg.EEG3),
                        Channel.TP10 to packet.getEegChannelValue(Eeg.EEG4),
                        Channel.AUX to packet.getEegChannelValue(Eeg.AUX_RIGHT),
                    ),
                )
                launch { send(sample) }
            }
        }

        val connectionListener = object : MuseConnectionListener() {
            override fun receiveMuseConnectionPacket(packet: MuseConnectionPacket?, muse: Muse?) {
                // No-op, but could be forwarded to the UI for status updates.
            }
        }

        fun Muse.bindListeners() {
            unregisterAllListeners()
            registerConnectionListener(connectionListener)
            registerDataListener(dataListener, MuseDataPacketType.EEG)
            runAsynchronously()
        }

        fun pickMuse(): Muse? {
            val muses = manager.muses
            if (muses.isEmpty()) return null
            preferredMac?.let { mac ->
                muses.firstOrNull { it.macAddress == mac }?.let { return it }
            }
            return muses.first()
        }

        val connectRunnable = Runnable {
            if (isClosed.get()) return@Runnable
            val muse = pickMuse()
            if (muse != null) {
                muse.bindListeners()
            } else {
                // Retry discovery every few seconds until a device is seen.
                mainHandler.postDelayed({ mainHandler.post(connectRunnable) }, 3_000)
            }
        }

        mainHandler.post(connectRunnable)

        awaitClose {
            isClosed.set(true)
            manager.muses.forEach { muse ->
                muse.unregisterAllListeners()
                muse.disconnect()
            }
            manager.stopListening()
        }
    }
}
