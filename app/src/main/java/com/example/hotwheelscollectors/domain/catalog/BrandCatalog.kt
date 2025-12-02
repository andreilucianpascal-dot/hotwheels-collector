package com.example.hotwheelscollectors.domain.catalog

object BrandCatalog {

    private val categoryToBrands: Map<String, List<Pair<String, String>>> = mapOf(
        "rally" to listOf(
            "audi" to "Audi", "bmw" to "BMW", "citroen" to "Citroen", "datsun" to "Datsun",
            "ford" to "Ford", "lancia" to "Lancia", "mazda" to "Mazda", "mitsubishi" to "Mitsubishi",
            "nissan" to "Nissan", "opel" to "Opel", "peugeot" to "Peugeot", "subaru" to "Subaru",
            "toyota" to "Toyota", "volkswagen" to "Volkswagen", "volvo" to "Volvo"
        ),
        "supercars" to listOf(
            "aston_martin" to "Aston Martin", "automobili_pininfarina" to "Automobili Pininfarina",
            "bentley" to "Bentley", "bugatti" to "Bugatti", "corvette" to "Corvette",
            "ferrari" to "Ferrari", "ford_gt" to "Ford GT", "koenigsegg" to "Koenigsegg",
            "lamborghini" to "Lamborghini", "lucid_air" to "Lucid Air", "maserati" to "Maserati",
            "mazda_787b" to "Mazda 787B", "mclaren" to "McLaren", "pagani" to "Pagani",
            "porsche" to "Porsche", "rimac" to "Rimac"
        ),
        "american_muscle" to listOf(
            "barracuda" to "Barracuda", "buick" to "Buick", "cadillac" to "Cadillac",
            "camaro" to "Camaro", "challenger" to "Challenger", "charger" to "Charger",
            "chevelle" to "Chevelle", "chevy" to "Chevy", "chevrolet" to "Chevrolet",
            "chrysler" to "Chrysler", "corvette" to "Corvette", "cougar" to "Cougar",
            "dodge" to "Dodge", "el_camino" to "El Camino", "firebird" to "Firebird",
            "ford" to "Ford", "gto" to "GTO", "impala" to "Impala", "lincoln" to "Lincoln",
            "mercury" to "Mercury", "mustang" to "Mustang", "nova" to "Nova",
            "oldsmobile" to "Oldsmobile", "plymouth" to "Plymouth", "pontiac" to "Pontiac",
            "super_bee" to "Super Bee", "thunderbird" to "Thunderbird"
        ),
        "vans" to listOf(
            "chevrolet" to "Chevrolet", "chrysler" to "Chrysler", "dodge" to "Dodge",
            "ford" to "Ford", "honda" to "Honda", "mercedes" to "Mercedes",
            "mercedes_benz" to "Mercedes", "nissan" to "Nissan", "toyota" to "Toyota",
            "volkswagen" to "Volkswagen"
        ),
        "convertibles" to listOf(
            "abarth" to "Abarth", "acura" to "Acura", "alfa_romeo" to "Alfa Romeo",
            "aston_martin" to "Aston Martin", "audi" to "Audi", "bentley" to "Bentley",
            "bmw" to "BMW", "bugatti" to "Bugatti", "cadillac" to "Cadillac",
            "chevrolet" to "Chevrolet", "chrysler" to "Chrysler", "citroen" to "Citroen",
            "corvette" to "Corvette", "daihatsu" to "Daihatsu", "datsun" to "Datsun",
            "dodge" to "Dodge", "ferrari" to "Ferrari", "fiat" to "Fiat",
            "ford" to "Ford", "honda" to "Honda", "infiniti" to "Infiniti",
            "jaguar" to "Jaguar", "koenigsegg" to "Koenigsegg", "lamborghini" to "Lamborghini",
            "land_rover" to "Land Rover", "lancia" to "Lancia", "lexus" to "Lexus",
            "lincoln" to "Lincoln", "lotus" to "Lotus", "maserati" to "Maserati",
            "mazda" to "Mazda", "mclaren" to "McLaren", "mercedes" to "Mercedes",
            "mercury" to "Mercury", "mini" to "Mini", "mitsubishi" to "Mitsubishi",
            "nissan" to "Nissan", "oldsmobile" to "Oldsmobile", "opel" to "Opel",
            "pagani" to "Pagani", "peugeot" to "Peugeot", "plymouth" to "Plymouth",
            "pontiac" to "Pontiac", "porsche" to "Porsche", "renault" to "Renault",
            "subaru" to "Subaru", "suzuki" to "Suzuki", "toyota" to "Toyota",
            "volkswagen" to "Volkswagen", "volvo" to "Volvo"
        ),
        "suv_trucks" to listOf(
            "audi" to "Audi", "bmw" to "BMW", "chevrolet" to "Chevrolet",
            "dodge" to "Dodge", "ford" to "Ford", "gmc" to "GMC", "honda" to "Honda",
            "hummer" to "Hummer", "jeep" to "Jeep", "land_rover" to "Land Rover",
            "mercedes" to "Mercedes", "mercedes_benz" to "Mercedes", "nissan" to "Nissan",
            "porsche" to "Porsche", "ram" to "Ram", "toyota" to "Toyota",
            "volkswagen" to "Volkswagen"
        ),
        "motorcycle" to listOf(
            "bmw" to "BMW", "ducati" to "Ducati", "harley_davidson" to "Harley Davidson",
            "honda" to "Honda", "indian" to "Indian", "kawasaki" to "Kawasaki",
            "suzuki" to "Suzuki", "triumph" to "Triumph", "yamaha" to "Yamaha"
        ),
        "hot_roads" to emptyList()
    )

    fun getBrandsForCategory(categoryId: String): List<Pair<String, String>> {
        return categoryToBrands[categoryId.lowercase()] ?: emptyList()
    }

    fun getAllBrandDisplayNames(): List<String> {
        return categoryToBrands.values
            .flatten()
            .map { it.second }
            .distinct()
            .sorted()
    }
}

