package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PileDao {

    @Query("SELECT * FROM piles ORDER BY createdAt DESC")
    fun getAllPiles(): Flow<List<PileEntity>>

    @Query("SELECT * FROM piles WHERE id = :id")
    fun getPileById(id: Long): Flow<PileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pile: PileEntity): Long

    @Query("DELETE FROM piles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
