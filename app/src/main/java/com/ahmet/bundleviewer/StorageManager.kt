package com.ahmet.bundleviewer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class StorageManager(private val context: Context) {

    private val fileName = "stored_bundles.json"
    private val gson = Gson()
    
    // The keys are the names of the bundles
    fun loadStoredBundles(): List<String> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()
        
        return try {
            val reader = FileReader(file)
            val type = object : TypeToken<List<String>>() {}.type
            val result: List<String> = gson.fromJson(reader, type) ?: emptyList()
            reader.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun saveStoredBundles(bundles: List<String>) {
        val file = File(context.filesDir, fileName)
        try {
            val writer = FileWriter(file)
            gson.toJson(bundles, writer)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
