package com.example.schedify

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val allSchedules: Flow<List<Schedule>> = _schedules.asStateFlow()

    fun getSchedulesByDay(day: String): Flow<List<Schedule>> {
        return MutableStateFlow(_schedules.value.filter { it.day == day }).asStateFlow()
    }

    fun insertSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val updatedList = _schedules.value.toMutableList()
            val newId = if (updatedList.isEmpty()) 1 else updatedList.maxOf { it.id } + 1
            updatedList.add(schedule.copy(id = newId))
            _schedules.value = updatedList
        }
    }

    fun updateSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val updatedList = _schedules.value.toMutableList()
            val index = updatedList.indexOfFirst { it.id == schedule.id }
            if (index != -1) {
                updatedList[index] = schedule
                _schedules.value = updatedList
            }
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val updatedList = _schedules.value.toMutableList()
            updatedList.removeAll { it.id == schedule.id }
            _schedules.value = updatedList
        }
    }

    fun deleteAllSchedules() {
        viewModelScope.launch {
            _schedules.value = emptyList()
        }
    }

    fun initializeDefaultSchedules() {
        viewModelScope.launch {
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
            _schedules.value = defaultSchedules
        }
    }
}
