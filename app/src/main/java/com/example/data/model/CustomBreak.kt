package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class CustomBreak(
    val type: String,          // "Çay", "Yemek", "İzin"
    val startTime: String,     // e.g., "10:15"
    val endTime: String,       // e.g., "10:30"
    val durationMinutes: Int
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("type", type)
        obj.put("startTime", startTime)
        obj.put("endTime", endTime)
        obj.put("durationMinutes", durationMinutes)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): CustomBreak {
            return CustomBreak(
                type = obj.getString("type"),
                startTime = obj.getString("startTime"),
                endTime = obj.getString("endTime"),
                durationMinutes = obj.getInt("durationMinutes")
            )
        }

        fun toJsonArrayString(list: List<CustomBreak>): String {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJsonObject()) }
            return arr.toString()
        }

        fun fromJsonArrayString(jsonStr: String?): List<CustomBreak> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            return try {
                val list = mutableListOf<CustomBreak>()
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    list.add(fromJsonObject(arr.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
