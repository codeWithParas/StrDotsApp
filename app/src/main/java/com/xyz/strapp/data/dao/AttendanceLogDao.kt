package com.xyz.strapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xyz.strapp.domain.model.entity.AttendanceLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceLogDao {

    /**
     * Inserts attendance logs into the database. If a conflict occurs (e.g., same ID),
     * it replaces the old entry.
     *
     * @param attendanceLogs List of AttendanceLogEntity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceLogs(attendanceLogs: List<AttendanceLogEntity>)

    /**
     * Inserts a single attendance log into the database.
     *
     * @param attendanceLog The AttendanceLogEntity to insert.
     * @return The row ID of the newly inserted log.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceLog(attendanceLog: AttendanceLogEntity): Long

    /**
     * Retrieves all attendance logs ordered by date_time in descending order (newest first).
     *
     * @return A Flow of list of AttendanceLogEntity objects.
     */
    @Query("SELECT * FROM attendance_logs ORDER BY date_time DESC")
    fun getAllAttendanceLogs(): Flow<List<AttendanceLogEntity>>

    /**
     * Retrieves attendance logs for a specific employee code.
     *
     * @param employeeCode The employee code to filter by.
     * @return A Flow of list of AttendanceLogEntity objects for the specific employee.
     */
    @Query("SELECT * FROM attendance_logs WHERE employee_code = :employeeCode ORDER BY date_time DESC")
    fun getAttendanceLogsByEmployeeCode(employeeCode: String): Flow<List<AttendanceLogEntity>>

    /**
     * Retrieves attendance logs for a specific action (e.g., "CheckIn", "CheckOut").
     *
     * @param action The action to filter by.
     * @return A Flow of list of AttendanceLogEntity objects for the specific action.
     */
    @Query("SELECT * FROM attendance_logs WHERE action = :action ORDER BY date_time DESC")
    fun getAttendanceLogsByAction(action: String): Flow<List<AttendanceLogEntity>>

    /**
     * Retrieves attendance logs within a specific date range.
     *
     * @param startDate The start date in string format.
     * @param endDate The end date in string format.
     * @return A Flow of list of AttendanceLogEntity objects within the date range.
     */
    @Query("SELECT * FROM attendance_logs WHERE date_time BETWEEN :startDate AND :endDate ORDER BY date_time DESC")
    fun getAttendanceLogsByDateRange(startDate: String, endDate: String): Flow<List<AttendanceLogEntity>>

    /**
     * Deletes all attendance logs from the database.
     */
    @Query("DELETE FROM attendance_logs")
    suspend fun deleteAllAttendanceLogs()

    /**
     * Deletes attendance logs older than a specific timestamp.
     *
     * @param timestamp The timestamp before which logs should be deleted.
     * @return The number of rows deleted.
     */
    @Query("DELETE FROM attendance_logs WHERE created_at < :timestamp")
    suspend fun deleteOldAttendanceLogs(timestamp: Long): Int

    /**
     * Gets the count of attendance logs in the database.
     *
     * @return The total count of attendance logs.
     */
    @Query("SELECT COUNT(*) FROM attendance_logs")
    suspend fun getAttendanceLogsCount(): Int
}
