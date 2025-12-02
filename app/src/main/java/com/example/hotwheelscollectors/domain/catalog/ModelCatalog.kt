package com.example.hotwheelscollectors.domain.catalog

object ModelCatalog {

    private val commonModels = listOf(
        // Ford Models
        "'32 Ford Roadster", "'40 Ford", "'49 Ford", "'55 Ford", "'56 Ford", "'57 Ford", "'60 Ford", "'65 Ford", "'66 Ford", "'67 Ford", "'68 Ford", "'69 Ford", "'70 Ford", "'71 Ford", "'72 Ford",
        "Mustang GT", "Mustang Shelby", "Mustang Fastback", "Mustang Mach 1", "Mustang Boss 302", "Mustang Boss 429", "Mustang Cobra", "Mustang Bullitt", "Mustang Eleanor",
        "F-150", "F-150 Lightning", "F-150 Raptor", "F-250", "F-350", "F-450", "F-650",
        "GT40", "GT500", "Bronco", "Bronco Sport", "Focus RS", "Fiesta ST", "Explorer", "Expedition", "Escape", "Edge", "Transit",
        "Thunderbird", "Torino", "Galaxie", "Falcon", "Ranger", "Maverick", "Contour", "Crown Victoria", "LTD", "Fairlane", "Gran Torino",

        // Chevrolet Models
        "'55 Chevy Bel Air", "'57 Chevy Bel Air", "'58 Chevy", "'59 Chevy", "'60 Chevy", "'61 Chevy", "'62 Chevy", "'63 Chevy", "'64 Chevy", "'65 Chevy", "'66 Chevy", "'67 Chevy", "'68 Chevy", "'69 Chevy", "'70 Chevy",
        "Camaro SS", "Camaro Z28", "Camaro IROC-Z", "Camaro ZL1", "Camaro 1LE", "Camaro Convertible", "Camaro Berlinetta", "Camaro Rally Sport",
        "Corvette Stingray", "Corvette Z06", "Corvette ZR1", "Corvette C8", "Corvette C7", "Corvette C6", "Corvette C5", "Corvette C4", "Corvette C3", "Corvette C2", "Corvette C1",
        "Chevelle SS", "Chevelle Malibu", "Chevelle 396", "Chevelle 454", "Chevelle Laguna", "Chevelle El Camino", "Chevelle Wagon",
        "Nova SS", "Nova 396", "Nova 350", "Nova Wagon", "Nova Yenko",
        "Impala", "Impala SS", "Impala Wagon", "Impala Convertible",
        "Silverado", "Silverado 1500", "Silverado 2500", "Silverado 3500", "Silverado Z71", "Silverado RST", "Silverado Trail Boss",
        "Blazer", "Suburban", "Tahoe", "Malibu", "Monte Carlo", "Colorado", "Cruze", "Sonic", "Spark", "Trax", "Equinox", "Traverse",
        "Avalanche", "Express", "S-10", "LUV", "El Camino", "Vega", "Chevette", "Citation", "Celebrity", "Caprice", "Lumina", "Beretta", "Corsica",

        // Dodge Models
        "Challenger", "Challenger SRT", "Challenger Hellcat", "Challenger Demon", "Challenger Redeye", "Challenger Scat Pack",
        "Charger", "Charger R/T", "Charger SRT", "Charger Hellcat", "Charger Daytona", "Charger Super Bee",
        "Viper", "Viper GTS", "Viper ACR", "Viper SRT-10", "Viper RT/10",
        "Dart", "Dart GT", "Dart Sport", "Dart Swinger", "Dart Demon",
        "Coronet", "Coronet R/T", "Coronet Super Bee", "Coronet 440", "Coronet 500",
        "Ram 1500", "Ram 2500", "Ram 3500", "Ram 4500", "Ram 5500", "Ram TRX", "Ram Rebel",
        "Durango", "Journey", "Grand Caravan", "Caravan", "Nitro", "Caliber", "Neon", "Stratus", "Intrepid", "Magnum", "Avenger", "Dakota",

        // BMW Models
        "M3", "M3 E30", "M3 E36", "M3 E46", "M3 E90", "M3 E92", "M3 F80", "M3 G80",
        "M5", "M5 E28", "M5 E34", "M5 E39", "M5 E60", "M5 F10", "M5 F90", "M5 G90",
        "X5", "X5 M", "X6", "X6 M", "X3", "X3 M", "X4", "X4 M", "X7", "X1", "X2",
        "i8", "i3", "iX", "i4", "iX3", "iX5",
        "Z4", "Z3", "Z8", "Z1", "Z4 M", "Z4 sDrive", "Z4 M40i",
        "7 Series", "6 Series", "5 Series", "4 Series", "3 Series", "2 Series", "1 Series",
        "8 Series", "M2", "M4", "M6", "M8", "M1", "M Roadster", "M Coupe",

        // Mercedes Models
        "AMG GT", "AMG GT R", "AMG GT S", "AMG GT C", "AMG GT 63", "AMG GT 53", "AMG GT Black Series",
        "S-Class", "S-Class AMG", "S-Class Maybach", "S-Class Coupe", "S-Class Convertible",
        "E-Class", "E-Class AMG", "E-Class Coupe", "E-Class Convertible", "E-Class Wagon",
        "C-Class", "C-Class AMG", "C-Class Coupe", "C-Class Convertible", "C-Class Wagon",
        "G-Class", "G-Class AMG", "G-Class 4x4", "G-Class Professional",
        "SL-Class", "SL-Class AMG", "SLK-Class", "SLC-Class",
        "CLS-Class", "CLS-Class AMG", "CLS-Class Shooting Brake",
        "A-Class", "A-Class AMG", "B-Class", "CLA-Class", "GLA-Class", "GLB-Class", "GLC-Class", "GLE-Class", "GLS-Class",
        "CL-Class", "CLK-Class", "R-Class", "M-Class", "GL-Class", "Sprinter", "V-Class", "Metris",

        // Porsche Models
        "911 GT3", "911 GT3 RS", "911 Turbo", "911 Turbo S", "911 Carrera", "911 Carrera S", "911 Carrera 4", "911 Carrera 4S",
        "911 Targa", "911 Targa 4S", "911 Cabriolet", "911 Speedster", "911 Dakar", "911 Sport Classic",
        "Cayenne", "Cayenne Turbo", "Cayenne Turbo S", "Cayenne GTS", "Cayenne S", "Cayenne E-Hybrid",
        "Macan", "Macan Turbo", "Macan GTS", "Macan S", "Macan T",
        "Panamera", "Panamera Turbo", "Panamera Turbo S", "Panamera GTS", "Panamera 4S", "Panamera E-Hybrid",
        "Taycan", "Taycan Turbo", "Taycan Turbo S", "Taycan 4S", "Taycan GTS", "Taycan Cross Turismo",
        "Boxster", "Boxster S", "Boxster GTS", "Boxster Spyder",
        "Cayman", "Cayman S", "Cayman GTS", "Cayman GT4", "Cayman GT4 RS",
        "718", "718 Boxster", "718 Cayman", "718 Spyder", "718 GT4", "718 GT4 RS",
        "918 Spyder", "Carrera GT", "959", "944", "928", "924", "914", "912", "911 T", "911 L", "911 E", "911 S",

        // Ferrari Models
        "F40", "F50", "Enzo", "LaFerrari", "LaFerrari Aperta", "SF90 Stradale", "SF90 Spider",
        "488 GTB", "488 Spider", "488 Pista", "488 Pista Spider",
        "F8 Tributo", "F8 Spider", "F8 Competizione",
        "Portofino", "Portofino M", "Roma", "Roma Spider",
        "812 Superfast", "812 GTS", "812 Competizione", "812 Competizione A",
        "296 GTB", "296 GTS", "296 GT3", "296 Challenge",
        "SF90 XX Stradale", "SF90 XX Spider", "Daytona SP3",
        "Monza SP1", "Monza SP2", "Icona Series",
        "California", "California T", "488 GT", "458 Italia", "458 Spider", "458 Speciale", "458 Speciale Aperta",
        "599 GTB", "599 GTO", "599 SA Aperta", "612 Scaglietti", "FF", "F12 Berlinetta", "F12tdf",

        // Lamborghini Models
        "Huracán", "Huracán Evo", "Huracán Evo Spyder", "Huracán STO", "Huracán Tecnica", "Huracán Sterrato",
        "Aventador", "Aventador S", "Aventador SVJ", "Aventador SVJ Roadster", "Aventador Ultimae",
        "Urus", "Urus S", "Urus Performante",
        "Gallardo", "Gallardo Superleggera", "Gallardo Spyder", "Gallardo LP 560-4", "Gallardo LP 570-4",
        "Murciélago", "Murciélago LP 640", "Murciélago LP 670-4 SV", "Murciélago Roadster",
        "Countach", "Countach LPI 800-4", "Countach Anniversary",
        "Diablo", "Diablo VT", "Diablo SV", "Diablo 6.0", "Diablo GT", "Diablo Roadster",
        "Revuelto", "Sián", "Sián FKP 37", "Sián Roadster",
        "Reventón", "Reventón Roadster", "Veneno", "Veneno Roadster", "Centenario", "Centenario Roadster",
        "Jalpa", "Espada", "Islero", "Jarama", "Urraco", "Silhouette", "LM002", "Miura", "350 GT", "400 GT",

        // Audi Models
        "R8", "R8 Spyder", "R8 GT", "R8 LMS", "R8 Performance", "R8 V10 Plus",
        "TT", "TT RS", "TT S", "TTS", "TT Roadster", "TT RS Roadster",
        "A1", "A3", "A3 Sportback", "A3 Sedan", "A3 Cabriolet", "A3 e-tron",
        "A4", "A4 Avant", "A4 Allroad", "A4 S-Line", "A4 Quattro",
        "A5", "A5 Sportback", "A5 Cabriolet", "A5 Coupe", "A5 S-Line",
        "A6", "A6 Avant", "A6 Allroad", "A6 S-Line", "A6 Quattro",
        "A7", "A7 Sportback", "A7 S-Line",
        "A8", "A8 L", "A8 S-Line", "A8 Quattro",
        "Q2", "Q3", "Q3 Sportback", "Q4 e-tron", "Q5", "Q5 Sportback", "Q5 e-tron",
        "Q7", "Q7 e-tron", "Q8", "Q8 Sportback", "e-tron", "e-tron GT", "e-tron Sportback",
        "RS3", "RS4", "RS5", "RS6", "RS7", "RS Q3", "RS Q8",
        "S3", "S4", "S5", "S6", "S7", "S8", "SQ2", "SQ5", "SQ7", "SQ8",

        // Volkswagen Models
        "Golf", "Golf GTI", "Golf R", "Golf GTD", "Golf e-Golf", "Golf SportWagen", "Golf Alltrack",
        "Jetta", "Jetta GLI", "Jetta SportWagen", "Jetta Hybrid",
        "Passat", "Passat CC", "Passat Wagon", "Passat Alltrack",
        "Beetle", "Beetle Convertible", "Beetle Dune", "Beetle Final Edition",
        "Tiguan", "Tiguan Allspace", "Atlas", "Atlas Cross Sport",
        "Touareg", "Arteon", "Arteon Shooting Brake", "CC", "Eos", "Phaeton",
        "ID.3", "ID.4", "ID.6", "ID.Buzz", "e-Up!", "e-Golf",
        "Scirocco", "Corrado", "Golf Cabriolet", "Rabbit", "Fox", "Lupo", "Polo", "Up!",

        // Japanese Brands - Toyota
        "Supra", "Supra A90", "Supra A80", "Supra A70", "Supra A60", "Supra A40",
        "Camry", "Camry Hybrid", "Camry TRD", "Camry SE", "Camry LE", "Camry XLE", "Camry XSE",
        "Corolla", "Corolla Hybrid", "Corolla GR", "Corolla SE", "Corolla LE", "Corolla XLE", "Corolla XSE",
        "Prius", "Prius Prime", "Prius C", "Prius V", "Prius Plug-in",
        "RAV4", "RAV4 Hybrid", "RAV4 Prime", "RAV4 TRD", "RAV4 Adventure",
        "Highlander", "Highlander Hybrid", "Highlander XSE", "Highlander Limited",
        "4Runner", "4Runner TRD Pro", "4Runner TRD Off-Road", "4Runner Limited",
        "Tacoma", "Tacoma TRD Pro", "Tacoma TRD Off-Road", "Tacoma Limited",
        "Tundra", "Tundra TRD Pro", "Tundra TRD Sport", "Tundra Limited",
        "Sequoia", "Sienna", "Avalon", "Avalon Hybrid", "Yaris", "Yaris GR", "86", "GR86",
        "Land Cruiser", "Land Cruiser Prado", "FJ Cruiser", "C-HR", "Venza", "Mirai",

        // Honda Models
        "Civic", "Civic Type R", "Civic Si", "Civic Hatchback", "Civic Sedan", "Civic Coupe", "Civic Hybrid",
        "Accord", "Accord Hybrid", "Accord Sport", "Accord Touring", "Accord EX-L",
        "CR-V", "CR-V Hybrid", "CR-V Sport", "CR-V Touring", "CR-V EX-L",
        "Pilot", "Pilot Elite", "Pilot Touring", "Pilot EX-L",
        "Passport", "Ridgeline", "Odyssey", "Insight", "Fit", "HR-V",
        "NSX", "NSX Type S", "S2000", "CR-Z", "Element", "Del Sol", "Prelude", "Integra", "RSX", "TSX", "TL", "RL", "MDX", "RDX",

        // Nissan Models
        "GT-R", "GT-R Nismo", "GT-R Premium", "GT-R Track Edition", "GT-R 50th Anniversary",
        "Skyline GT-R", "Skyline R32", "Skyline R33", "Skyline R34", "Skyline R35",
        "370Z", "370Z Nismo", "370Z Heritage Edition", "370Z Roadster",
        "350Z", "350Z Nismo", "350Z Track Edition", "350Z Roadster",
        "Fairlady Z", "Fairlady Z432", "Fairlady Z432R", "Fairlady ZG", "Fairlady Z S30", "Fairlady Z S130", "Fairlady Z Z31", "Fairlady Z Z32",
        "Sentra", "Altima", "Maxima", "Versa", "Kicks", "Rogue", "Rogue Sport", "Murano", "Pathfinder", "Armada",
        "Titan", "Titan XD", "Frontier", "Navara", "Leaf", "Ariya", "Juke", "Cube", "Quest", "NV200",
        "240SX", "180SX", "Silvia", "Silvia S13", "Silvia S14", "Silvia S15", "180SX Type X", "200SX",

        // Mazda Models
        "RX-7", "RX-7 FC", "RX-7 FD", "RX-7 SA", "RX-7 FB", "RX-7 GSL", "RX-7 GSL-SE", "RX-7 Turbo", "RX-7 Spirit R",
        "RX-8", "RX-8 R3", "RX-8 Shinka", "RX-8 GT", "RX-8 Sport", "RX-8 Touring",
        "Miata", "Miata MX-5", "Miata ND", "Miata NC", "Miata NB", "Miata NA", "Miata RF", "Miata Club", "Miata Grand Touring",
        "CX-3", "CX-5", "CX-9", "CX-30", "CX-50", "CX-60", "CX-70", "CX-90",
        "Mazda3", "Mazda3 Hatchback", "Mazda3 Sedan", "Mazda3 Turbo", "Mazda3 Carbon Edition",
        "Mazda6", "Mazda6 Turbo", "Mazda6 Carbon Edition", "Mazda6 Grand Touring",
        "CX-7", "Tribute", "MPV", "Mazda5", "B-Series", "RX-3", "RX-2", "RX-4", "Cosmo", "Eunos", "Xedos", "Millenia",

        // Subaru Models
        "WRX", "WRX STI", "WRX Premium", "WRX Limited", "WRX Sport", "WRX Base",
        "BRZ", "BRZ Premium", "BRZ Limited", "BRZ Sport", "BRZ tS",
        "Impreza", "Impreza WRX", "Impreza WRX STI", "Impreza Sport", "Impreza Premium", "Impreza Limited",
        "Legacy", "Legacy Sport", "Legacy Premium", "Legacy Limited", "Legacy Touring XT",
        "Outback", "Outback Wilderness", "Outback Onyx", "Outback Limited", "Outback Touring XT",
        "Forester", "Forester Sport", "Forester Premium", "Forester Limited", "Forester Touring",
        "Ascent", "Ascent Premium", "Ascent Limited", "Ascent Touring",
        "Crosstrek", "Crosstrek Sport", "Crosstrek Premium", "Crosstrek Limited", "Crosstrek Hybrid",
        "SVX", "XT", "XT6", "XT Turbo", "Justy", "Loyale", "Legacy Outback", "Baja", "Tribeca",

        // Mitsubishi Models
        "Lancer Evolution", "Lancer Evo X", "Lancer Evo IX", "Lancer Evo VIII", "Lancer Evo VII", "Lancer Evo VI", "Lancer Evo V", "Lancer Evo IV", "Lancer Evo III", "Lancer Evo II", "Lancer Evo I",
        "Lancer", "Lancer Ralliart", "Lancer Sportback", "Lancer GTS", "Lancer ES",
        "Eclipse", "Eclipse Cross", "Eclipse Spyder", "Eclipse GT", "Eclipse GSX", "Eclipse GST",
        "Outlander", "Outlander Sport", "Outlander PHEV", "Outlander GT", "Outlander SEL",
        "Mirage", "Mirage G4", "Mirage ES", "Mirage LE",
        "Galant", "Galant VR-4", "Galant ES", "Galant GTZ",
        "Diamante", "3000GT", "3000GT VR-4", "3000GT SL", "3000GT Spyder",
        "Montero", "Montero Sport", "Montero Limited", "Montero SR",
        "Endeavor", "Expo", "Expo LRV", "Expo Wagon", "Cordia", "Tredia", "Starion", "Conquest",

        // Hot Wheels Fantasy Cars
        "Bone Shaker", "Twin Mill", "Deora II", "Sling Shot", "16 Angels", "What-4-2",
        "Twinduction", "Carbonic", "Synkro", "Night Shifter", "Rip Rod", "Speed Blaster",
        "Jet Threat", "Splittin' Image", "Power Pistons", "Altered State", "Barbaric", "Bedlam",
        "Mad Manga", "Mystery Machine", "Knight Rider", "A-Team Van", "General Lee", "Ghostbusters Ecto-1",
        "Jurassic Park Explorer", "Back to the Future Time Machine", "Herbie", "Lightning McQueen",
        "Mater", "Sally", "Doc Hudson", "Strip Weathers", "Chick Hicks", "Francesco Bernoulli",
        "Jackson Storm", "Cruz Ramirez", "Sterling", "Cal Weathers", "Bobby Swift", "Brick Yardley",

        // Other European Sports Cars
        "McLaren F1", "McLaren 720S", "McLaren P1", "McLaren 600LT", "McLaren 570S", "McLaren 540C",
        "McLaren 650S", "McLaren 675LT", "McLaren 765LT", "McLaren Artura", "McLaren Senna", "McLaren Speedtail",
        "Bugatti Veyron", "Bugatti Chiron", "Bugatti Divo", "Bugatti Centodieci", "Bugatti La Voiture Noire",
        "Bugatti Bolide", "Bugatti Mistral", "Bugatti W16 Mistral", "Bugatti EB110", "Bugatti Type 35",
        "Koenigsegg Agera", "Koenigsegg Regera", "Koenigsegg Jesko", "Koenigsegg Gemera", "Koenigsegg CC8S",
        "Koenigsegg CCX", "Koenigsegg CCR", "Koenigsegg CCGT", "Koenigsegg One:1", "Koenigsegg CC850",
        "Pagani Huayra", "Pagani Zonda", "Pagani Utopia", "Pagani Imola", "Pagani Huayra BC",
        "Pagani Zonda R", "Pagani Zonda F", "Pagani Zonda Cinque", "Pagani Zonda Tricolore",
        "Rimac Nevera", "Rimac Concept One", "Rimac C_Two", "Rimac Verne",
        "Lotus Elise", "Lotus Exige", "Lotus Evora", "Lotus Emira", "Lotus Esprit", "Lotus Europa",
        "Lotus Evija", "Lotus Type 131", "Lotus 3-Eleven", "Lotus 2-Eleven", "Lotus 340R",

        // American Muscle and Sports Cars
        "Pontiac GTO", "Pontiac Firebird", "Pontiac Trans Am", "Pontiac Grand Prix", "Pontiac Bonneville",
        "Pontiac Catalina", "Pontiac Tempest", "Pontiac LeMans", "Pontiac Grand Am", "Pontiac Sunfire",
        "Buick GNX", "Buick Grand National", "Buick Regal", "Buick Riviera", "Buick LeSabre",
        "Buick Roadmaster", "Buick Park Avenue", "Buick Century", "Buick Skylark", "Buick Electra",
        "Cadillac CTS-V", "Cadillac ATS-V", "Cadillac CT5-V", "Cadillac CT4-V", "Cadillac Escalade",
        "Cadillac XT4", "Cadillac XT5", "Cadillac XT6", "Cadillac CT6", "Cadillac ELR",
        "Oldsmobile Cutlass", "Oldsmobile 442", "Oldsmobile Toronado", "Oldsmobile Delta 88",
        "Oldsmobile Ninety-Eight", "Oldsmobile Intrigue", "Oldsmobile Aurora", "Oldsmobile Alero",
        "Plymouth Barracuda", "Plymouth 'Cuda", "Plymouth Road Runner", "Plymouth Superbird",
        "Plymouth GTX", "Plymouth Satellite", "Plymouth Fury", "Plymouth Duster", "Plymouth Valiant",
        "Lincoln Continental", "Lincoln Mark VIII", "Lincoln Navigator", "Lincoln Aviator",
        "Lincoln Corsair", "Lincoln Nautilus", "Lincoln MKZ", "Lincoln MKX", "Lincoln MKC",
        "Mercury Cougar", "Mercury Marauder", "Mercury Montego", "Mercury Cyclone",
        "Mercury Comet", "Mercury Capri", "Mercury Sable", "Mercury Grand Marquis",

        // Modern Electric and Hybrid
        "Tesla Model S", "Tesla Model 3", "Tesla Model X", "Tesla Model Y", "Tesla Roadster",
        "Tesla Cybertruck", "Tesla Semi", "Tesla Plaid", "Tesla Long Range", "Tesla Performance",
        "Rivian R1T", "Rivian R1S", "Rivian Amazon Delivery Van", "Rivian Adventure Package",
        "Lucid Air", "Lucid Air Dream Edition", "Lucid Air Grand Touring", "Lucid Air Touring",
        "Polestar 1", "Polestar 2", "Polestar 3", "Polestar 4", "Polestar 5", "Polestar 6",
        "Genesis GV70", "Genesis GV80", "Genesis G70", "Genesis G80", "Genesis G90", "Genesis Electrified GV70",
        "Hyundai Ioniq 5", "Hyundai Ioniq 6", "Hyundai Kona Electric", "Hyundai Nexo",
        "Kia EV6", "Kia EV9", "Kia Niro EV", "Kia Soul EV", "Kia Optima Hybrid",

        // Classic and Vintage
        "'32 Ford Roadster", "'40 Ford", "'49 Ford", "'55 Ford", "'56 Ford", "'57 Ford", "'60 Ford", "'65 Ford", "'66 Ford", "'67 Ford", "'68 Ford", "'69 Ford", "'70 Ford",
        "'55 Chevy Bel Air", "'57 Chevy Bel Air", "'58 Chevy", "'59 Chevy", "'60 Chevy", "'61 Chevy", "'62 Chevy", "'63 Chevy", "'64 Chevy", "'65 Chevy", "'66 Chevy", "'67 Chevy", "'68 Chevy", "'69 Chevy", "'70 Chevy",
        "'69 Dodge Charger", "'70 Dodge Charger", "'71 Dodge Charger", "'68 Dodge Dart", "'69 Dodge Dart", "'70 Dodge Dart",
        "'67 Camaro", "'68 Camaro", "'69 Camaro", "'70 Camaro", "'71 Camaro", "'72 Camaro", "'73 Camaro", "'74 Camaro",
        "'65 Mustang", "'66 Mustang", "'67 Mustang", "'68 Mustang", "'69 Mustang", "'70 Mustang", "'71 Mustang", "'72 Mustang",
        "'63 Corvette", "'64 Corvette", "'65 Corvette", "'66 Corvette", "'67 Corvette", "'68 Corvette", "'69 Corvette", "'70 Corvette",
        "'69 GTO", "'70 GTO", "'71 GTO", "'72 GTO", "'73 GTO", "'74 GTO",
        "'69 Firebird", "'70 Firebird", "'71 Firebird", "'72 Firebird", "'73 Firebird", "'74 Firebird", "'75 Firebird", "'76 Firebird"
    ).sorted()

    fun getCommonModels(): List<String> = commonModels
}

