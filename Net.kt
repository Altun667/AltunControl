package com.altuncontrol.app

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object Net {
    // Wake-on-LAN paketini dogrudan telefon gonderir
    fun wol(mac: String) {
        try {
            val hex = mac.replace(":", "").replace("-", "")
            val m = ByteArray(6) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
            val pkt = ByteArray(6 + 16 * 6)
            for (i in 0 until 6) pkt[i] = 0xFF.toByte()
            for (i in 0 until 16) System.arraycopy(m, 0, pkt, 6 + i * 6, 6)
            DatagramSocket().use { s ->
                s.broadcast = true
                for (port in intArrayOf(9, 7)) {
                    s.send(DatagramPacket(pkt, pkt.size, InetAddress.getByName("255.255.255.255"), port))
                }
            }
        } catch (_: Exception) {
        }
    }
}
