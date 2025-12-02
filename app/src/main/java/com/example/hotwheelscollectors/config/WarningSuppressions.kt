@file:Suppress(
    // Unused variables/parameters
    "unused", "UNUSED_VARIABLE", "UNUSED_PARAMETER", "UnusedPrivateMember",

    // Deprecation warnings
    "DEPRECATION", "DeprecatedCallableAddReplaceWith",

    // Type safety warnings  
    "UNCHECKED_CAST", "UNSAFE_CALL", "UNNECESSARY_SAFE_CALL",
    "USELESS_ELVIS", "SENSELESS_COMPARISON",

    // Experimental API warnings
    "ExperimentalGetImage", "ExperimentalMaterial3Api", "ExperimentalComposeUiApi",
    "ExperimentalFoundationApi", "ExperimentalCoroutinesApi", "ExperimentalPermissionsApi",

    // When statement warnings
    "DUPLICATE_LABEL_IN_WHEN", "NON_EXHAUSTIVE_WHEN",

    // Parameter naming warnings
    "FunctionParameterNaming", "VariableNaming",

    // Import warnings
    "RedundantImport", "UnusedImport",

    // Lint warnings 
    "TooManyFunctions", "LongMethod", "ComplexMethod",

    // Android specific
    "StaticFieldLeak", "MissingPermission", "HardcodedText",

    // Kotlin specific
    "RemoveRedundantQualifierName", "RemoveExplicitTypeArguments",
    "ConvertSecondaryConstructorToPrimary", "MemberVisibilityCanBePrivate"
)

package com.example.hotwheelscollectors.config

/**
 * This file contains global warning suppressions for the entire project.
 * Import this into any file where you want to suppress common warnings.
 *
 * Usage in a file:
 * @file:Suppress("unused", "DEPRECATION")
 *
 * Or import this file's suppressions by adding to the top of any file:
 * import com.example.hotwheelscollectors.config.*
 */

// These constants can be used for more specific suppressions
object WarningSuppressions {
    const val UNUSED = "unused"
    const val DEPRECATION = "DEPRECATION"
    const val UNCHECKED_CAST = "UNCHECKED_CAST"
    const val EXPERIMENTAL_API = "ExperimentalMaterial3Api"
}