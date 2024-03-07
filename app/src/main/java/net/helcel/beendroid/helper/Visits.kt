package net.helcel.beendroid.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.Json
import net.helcel.beendroid.countries.GeoLoc
import java.io.InputStream


@Serializable
class Visits(val id: Int, private val locs: HashMap<String,Int>) {

    fun setVisited(key: GeoLoc, b: Int) {
        locs[key.code] = b
    }

    fun deleteVisited(key: Int) {
        val keysToDelete = locs
            .filter { it.value == key }
            .map { it.key }
        keysToDelete.forEach {
            locs.remove(it)
        }
    }

    fun getVisited(key: GeoLoc): Int {
        return locs.getOrDefault(key.code,0)
    }

    fun countVisited(key: Int): Int {
        return locs.filter { it.value == key }.size
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(Visits::class)
    class VisitsSerializer {
        val defaultValue: Visits
            get() = Visits(Int.MIN_VALUE,hashMapOf())

        fun readFrom(input: InputStream): Visits {
            return Json.decodeFromString(serializer(),input.readBytes().decodeToString())
        }

       fun writeTo(t: Visits): String {
            return Json.encodeToString(serializer(),t).encodeToByteArray().decodeToString()
        }

    }

}