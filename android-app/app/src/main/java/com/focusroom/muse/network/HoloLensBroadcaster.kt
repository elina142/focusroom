package com.focusroom.muse.network

import com.focusroom.muse.model.AttentionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class HoloLensBroadcaster(
    private val host: String,
    private val port: Int,
) {
    private val inetAddress: InetAddress = InetAddress.getByName(host)

    suspend fun send(result: AttentionResult) = withContext(Dispatchers.IO) {
        val payload = JSONObject(
            mapOf(
                "score" to result.normalizedScore,
                "level" to result.discreteLevel,
                "lowEnergy" to result.lowFrequencyEnergy,
                "fastEnergy" to result.fastChangeEnergy,
            ),
        ).toString()

        DatagramSocket().use { socket ->
            val bytes = payload.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(bytes, bytes.size, inetAddress, port)
            socket.send(packet)
        }
    }
}
