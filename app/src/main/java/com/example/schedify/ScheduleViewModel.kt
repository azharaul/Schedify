package com.example.schedify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.scheduleDao()

    val allSchedules: Flow<List<Schedule>> = dao.getAllSchedules()

    fun getSchedulesByDay(day: String): Flow<List<Schedule>> {
        return dao.getSchedulesByDay(day)
    }

    fun insertSchedule(schedule: Schedule) {
        viewModelScope.launch {
            // If id is 0, let Room auto-generate it. Otherwise, use the provided id (important for undo).
            if (schedule.id == 0) {
                dao.insert(schedule.copy(id = 0)) // Explicitly set to 0 to trigger auto-gen
            } else {
                dao.insert(schedule)
            }
        }
    }

    /**
     * Restore a schedule with its original id. Used for Undo so alarms/ids remain consistent.
     */
    fun restoreSchedule(schedule: Schedule) {
        viewModelScope.launch {
            dao.insert(schedule)
        }
    }

    fun updateSchedule(schedule: Schedule) {
        viewModelScope.launch {
            dao.update(schedule)
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            dao.delete(schedule)
        }
    }

    fun deleteAllSchedules() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }
}