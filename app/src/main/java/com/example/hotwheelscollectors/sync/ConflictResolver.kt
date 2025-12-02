// ConflictResolver.kt
package com.example.hotwheelscollectors.sync

import com.example.hotwheelscollectors.data.local.entities.CarEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConflictResolver @Inject constructor() {
    fun resolve(
        localChanges: List<CarEntity>,
        remoteChanges: List<CarEntity>
    ): Pair<List<CarEntity>, List<CarEntity>> {
        val resolvedLocal = mutableListOf<CarEntity>()
        val resolvedRemote = mutableListOf<CarEntity>()

        // Group changes by car ID
        val changesByCarId = (localChanges + remoteChanges)
            .groupBy { it.id }

        changesByCarId.forEach { (_, changes) ->
            when {
                // No conflict - only local change
                changes.size == 1 && changes[0] in localChanges -> {
                    resolvedLocal.add(changes[0])
                }

                // No conflict - only remote change
                changes.size == 1 && changes[0] in remoteChanges -> {
                    resolvedRemote.add(changes[0])
                }

                // Conflict - both local and remote changes
                changes.size == 2 -> {
                    val local = changes.first { it in localChanges }
                    val remote = changes.first { it in remoteChanges }

                    resolveConflict(local, remote).let { (resolvedL, resolvedR) ->
                        resolvedL?.let { resolvedLocal.add(it) }
                        resolvedR?.let { resolvedRemote.add(it) }
                    }
                }
            }
        }

        return Pair(resolvedLocal, resolvedRemote)
    }

    private fun resolveConflict(
        local: CarEntity,
        remote: CarEntity
    ): Pair<CarEntity?, CarEntity?> {
        // If one is deleted, prefer deletion
        if (local.isDeleted || remote.isDeleted) {
            return Pair(
                if (local.isDeleted) local else null,
                if (remote.isDeleted) remote else null
            )
        }

        // Compare versions and timestamps
        return when {
            local.version > remote.version -> Pair(local, null)
            remote.version > local.version -> Pair(null, remote)
            local.updatedAt > remote.updatedAt -> Pair(local, null)
            remote.updatedAt > local.updatedAt -> Pair(null, remote)
            else -> Pair(local, null) // Default to local changes
        }
    }
}