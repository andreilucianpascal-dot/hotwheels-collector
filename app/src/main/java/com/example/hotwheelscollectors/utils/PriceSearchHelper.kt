package com.example.hotwheelscollectors.utils

import android.content.Intent
import android.net.Uri
import com.example.hotwheelscollectors.data.local.entities.CarEntity
import com.example.hotwheelscollectors.data.repository.GlobalCarData
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Helper class for generating Google Shopping search links for price checking.
 * 
 * Uses Google Shopping which is optimized for product searches and price comparison.
 * Works globally (all regions) and shows real prices from multiple sources.
 */
object PriceSearchHelper {
    
    /**
     * Builds search query from car data for precise product search.
     * Example: "Hot Wheels Ferrari F40 2024 Premium"
     */
    private fun buildSearchQuery(car: CarEntity): String {
        val terms = mutableListOf<String>()
        
        // Always add "Hot Wheels" for better results
        terms.add("Hot Wheels")
        
        if (car.brand.isNotEmpty()) {
            terms.add(car.brand)
        }
        if (car.model.isNotEmpty()) {
            terms.add(car.model)
        }
        if (car.year > 0) {
            terms.add(car.year.toString())
        }
        if (car.series.isNotEmpty() && !car.series.equals("mainline", ignoreCase = true)) {
            terms.add(car.series)
        }
        if (car.color.isNotEmpty()) {
            terms.add(car.color)
        }
        
        return terms.joinToString(" ")
    }
    
    /**
     * Builds search query from GlobalCarData (for Browse cars).
     */
    private fun buildSearchQuery(car: GlobalCarData): String {
        val terms = mutableListOf<String>()
        
        // Always add "Hot Wheels" for better results
        terms.add("Hot Wheels")
        
        if (car.brand.isNotEmpty()) {
            terms.add(car.brand)
        }
        if (car.carName.isNotEmpty()) {
            terms.add(car.carName)
        }
        if (car.year > 0) {
            terms.add(car.year.toString())
        }
        // Add series if it's not "mainline" or empty
        if (car.series.isNotEmpty() && 
            !car.series.equals("mainline", ignoreCase = true) &&
            !car.series.equals("Mainline", ignoreCase = true)) {
            terms.add(car.series)
        }
        // Add barcode if available (helps with exact product matching)
        if (car.barcode.isNotEmpty()) {
            terms.add(car.barcode)
        }
        // Color is optional - only add if it helps narrow down results
        // (removed color from query as it can be too specific and reduce results)
        
        return terms.joinToString(" ")
    }
    
    /**
     * Opens Google Shopping search for a car from My Collection.
     * 
     * @param car CarEntity from My Collection
     * @return Intent to open Google Shopping with the search query
     */
    fun getGoogleShoppingIntent(car: CarEntity): Intent {
        val searchQuery = buildSearchQuery(car)
        val encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString())
        val shoppingUrl = "https://www.google.com/search?tbm=shop&q=$encodedQuery"
        
        return Intent(Intent.ACTION_VIEW, Uri.parse(shoppingUrl))
    }
    
    /**
     * Opens Google Shopping search for a car from Browse (GlobalCarData).
     * 
     * @param car GlobalCarData from Browse
     * @return Intent to open Google Shopping with the search query
     */
    fun getGoogleShoppingIntent(car: com.example.hotwheelscollectors.data.repository.GlobalCarData): Intent {
        val searchQuery = buildSearchQuery(car)
        val encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString())
        val shoppingUrl = "https://www.google.com/search?tbm=shop&q=$encodedQuery"
        
        return Intent(Intent.ACTION_VIEW, Uri.parse(shoppingUrl))
    }
}

