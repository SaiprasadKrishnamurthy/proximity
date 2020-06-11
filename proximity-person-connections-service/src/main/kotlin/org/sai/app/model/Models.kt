package org.sai.app.model

data class ProximityEventsIngestRequest(var userIdHash: String,
                                        val proximityEvents: List<ProximityEvent> = listOf())

data class ProximityEvent(val userIdHash: String,
                          val timestamp: Long = System.currentTimeMillis(),
                          val geoLocation: String,
                          val tags: List<String> = listOf(),
                          val channel: ChannelType = ChannelType.BluetoothSensing,
                          val canonicalLocationName: String = "",
                          val proximityType: ProximityType = ProximityType.In) {
    var count = 0

    init {
        count = if (proximityType == ProximityType.In || count == 0) 1 else -1
    }
}

enum class ChannelType {
    BluetoothSensing, Beacon, Gps
}

enum class ProximityType {
    In, Out
}

interface ProximityPersonConnectionsService {
    fun connectedPersons(userIdHash: String): PersonConnections
}

data class ProximalPersons(val userIdHash: String)

data class LocationDetails(val location:String ,
                           val timestamp: Long = System.currentTimeMillis() )

data class Connections(val source:Int, val target:Int, val properties: List<LocationDetails>)

data class PersonConnections(val persons:List<ProximalPersons>, val connections: List<Connections>)