package com.example.hotwheelscollectors.data.local

import com.example.hotwheelscollectors.model.HotWheelsCar

object CarDatabase {
    // All mainline brands that will always be shown (Hot Wheels only)
    fun getAllMainlineBrands(): List<String> = listOf(
        "Hot Wheels"
    ).sorted()

    // Models for each brand (Hot Wheels only)
    fun getModelsForBrand(brand: String): List<String> = when (brand.lowercase()) {
        "hot wheels" -> listOf(
            "All models are Hot Wheels"
        )
        else -> emptyList()
    }

    // Generate search keywords for a car
    fun generateSearchKeywords(car: HotWheelsCar): List<String> {
        val keywords = mutableSetOf<String>()

        // Add main fields
        car.name?.lowercase()?.let { keywords.add(it) }
        car.brand?.lowercase()?.let { keywords.add(it) }
        car.model?.lowercase()?.let { keywords.add(it) }
        car.series?.lowercase()?.let { keywords.add(it) }
        car.subseries?.lowercase()?.let { keywords.add(it) }
        car.color?.lowercase()?.let { keywords.add(it) }
        car.year?.toString()?.let { keywords.add(it) }
        car.number?.let { keywords.add(it) }
        car.barcode?.let { keywords.add(it) }

        // Add special designations
        if (car.isSTH) keywords.add("super treasure hunt")
        if (car.isTH) keywords.add("treasure hunt")
        if (car.isFirstEdition) keywords.add("first edition")
        if (car.isPremium) keywords.add("premium")

        // Split fields into individual words
        keywords.addAll(car.name?.split(" ")?.map { it.lowercase() } ?: emptyList())
        keywords.addAll(car.series?.split(" ")?.map { it.lowercase() } ?: emptyList())
        keywords.addAll(car.subseries?.split(" ")?.map { it.lowercase() } ?: emptyList())

        return keywords.toList()
    }

    // Get all premium series (Hot Wheels only)
    fun getAllPremiumSeries(): List<String> = listOf(
        "Car Culture",
        "Team Transport",
        "Premium Boulevard",
        "RLC",
        "STH",
        "Boulevard",
        "Fast & Furious",
        "Premium Collection",
        "Character Cars",
        "Entertainment",
        "Replica Entertainment",
        "Pop Culture",
        "Hot Wheels id"
    ).sorted()

    // Get subseries for a premium series (Hot Wheels only)
    fun getSubseriesForSeries(series: String): List<String> = when (series.lowercase()) {
        "car culture" -> listOf(
            "Modern Classic",
            "Race Day",
            "Circuit Legends",
            "Team Transport",
            "Silhouettes",
            "Jay Leno Garage",
            "RTR Vehicles",
            "Real Riders",
            "Fast Wagons",
            "Speed Machine",
            "Japan Historics",
            "Hammer Drop",
            "Slide Street",
            "Terra Trek",
            "Exotic Envy",
            "Cargo Containers"
        )
        "pop culture" -> listOf(
            "Fast and Furious",
            "Mario Kart",
            "Forza",
            "Gran Turismo",
            "Top Gun",
            "Batman",
            "Star Wars",
            "Marvel",
            "Jurassic World",
            "Back to the Future",
            "Looney Tunes"
        )
        "boulevard" -> emptyList()
        "f1" -> emptyList()
        "rlc" -> emptyList()
        "1:43 scale" -> emptyList()
        "others premium" -> emptyList()
        else -> emptyList()
    }
}