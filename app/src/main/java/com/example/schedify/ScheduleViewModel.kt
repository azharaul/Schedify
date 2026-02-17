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

    fun initializeDefaultSchedules() {
        viewModelScope.launch {
            // Check if database is empty before inserting defaults
            allSchedules.first().let { schedules ->
                if (schedules.isEmpty()) {
                    val defaultSchedules = listOf(
                        Schedule(1, "Pemrosesan Data Terdistribusi", "07:55 – 10:35", "Senin", "Ruang A101"),
                        Schedule(2, "Pemrograman Berbasis Platform", "13:00 – 16:35", "Senin", "Ruang B201"),
                        Schedule(3, "Riset dan Pengembangan", "07:55 – 11:30", "Selasa", "Lab 1"),
                        Schedule(4, "Kecerdasan Buatan", "13:00 – 16:35", "Selasa", "Lab 2"),
                        Schedule(5, "Pemrosesan Data Terdistribusi", "07:55 – 10:30", "Rabu", "Ruang A101"),
                        Schedule(6, "Dasar-Dasar Jaringan", "13:00 – 14:40", "Rabu", "Ruang C303"),
                        Schedule(7, "Bahasa Inggris 4", "08:00 – 09:40", "Kamis", "Ruang D401"),
                        Schedule(8, "Agama 4", "10:40 – 11:30", "Kamis", "Ruang D402"),
                        Schedule(9, "Dasar-Dasar Jaringan", "13:00 – 14:40", "Kamis", "Ruang C303"),
                        Schedule(10, "Pemrograman Berorientasi Objek", "14:50 – 15:40", "Kamis", "Ruang B201"),
                        Schedule(11, "Kewirausahaan Teknologi Informasi", "13:00 – 14:45", "Jumat", "Ruang E"),
                        Schedule(12, "Olah Raga", "08:00 – 10:00", "Sabtu", "Lapangan"),
                        Schedule(13, "Seni Budaya", "10:30 – 12:30", "Sabtu", "Studio"),
                        Schedule(14, "Istirahat", "00:00 – 23:59", "Minggu", "")
                    )
                    defaultSchedules.forEach { dao.insert(it) }
                }
            }
        }
    }
}