package com.example.data.repository

import com.example.data.db.MeasurementDao
import com.example.data.db.MeasurementEntity
import com.example.data.db.PileDao
import com.example.data.db.PileEntity
import kotlinx.coroutines.flow.Flow

class MeasurementRepository(
    private val dao: MeasurementDao,
    private val pileDao: PileDao
) {

    val allMeasurements: Flow<List<MeasurementEntity>> = dao.getAllMeasurements()
    val allPiles: Flow<List<PileEntity>> = pileDao.getAllPiles()

    fun measurementsForPile(pileId: Long): Flow<List<MeasurementEntity>> = dao.getByPileId(pileId)

    fun measurementById(id: Long): Flow<MeasurementEntity?> = dao.getById(id)

    fun pileById(pileId: Long): Flow<PileEntity?> = pileDao.getPileById(pileId)

    suspend fun insert(measurement: MeasurementEntity): Long {
        return dao.insert(measurement)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun insertPile(pile: PileEntity): Long {
        return pileDao.insert(pile)
    }

    suspend fun deletePile(id: Long) {
        dao.clearPileReferences(id)
        pileDao.deleteById(id)
    }
}
