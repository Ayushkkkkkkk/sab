package com.sab.pawcare.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.sab.pawcare.data.entity.ChecklistTask;

import java.util.List;

@Dao
public interface ChecklistTaskDao {
    @Insert
    long insert(ChecklistTask task);

    @Update
    void update(ChecklistTask task);

    @Delete
    void delete(ChecklistTask task);

    @Query("SELECT * FROM checklist_tasks WHERE id = :id LIMIT 1")
    ChecklistTask findById(long id);

    @Query("SELECT * FROM checklist_tasks WHERE ownerId = :ownerId AND taskDate = :date ORDER BY completed, scheduledMinutes, title")
    LiveData<List<ChecklistTask>> observeByDate(long ownerId, String date);

    @Query("SELECT * FROM checklist_tasks WHERE ownerId = :ownerId AND taskDate = :date ORDER BY completed, scheduledMinutes, title")
    List<ChecklistTask> listByDate(long ownerId, String date);

    @Query("SELECT COUNT(*) FROM checklist_tasks WHERE ownerId = :ownerId AND taskDate = :date AND routineId = :routineId")
    int countGenerated(long ownerId, String date, long routineId);

    @Query("UPDATE checklist_tasks SET completed = 0, completedAt = 0 WHERE ownerId = :ownerId AND taskDate = :date")
    void resetDate(long ownerId, String date);

    @Query("DELETE FROM checklist_tasks WHERE ownerId = :ownerId AND taskDate = :date AND customTask = 0")
    void deleteGeneratedForDate(long ownerId, String date);

    @Query("SELECT * FROM checklist_tasks WHERE ownerId = :ownerId AND taskDate = :date AND completed = 0")
    List<ChecklistTask> pendingForDate(long ownerId, String date);

    @Query("SELECT * FROM checklist_tasks WHERE ownerId = :ownerId AND taskDate = :date AND completed = 1")
    List<ChecklistTask> completedForDate(long ownerId, String date);

    @Query("SELECT COUNT(*) FROM checklist_tasks WHERE ownerId = :ownerId AND taskDate = :date AND completed = 0")
    int countPending(long ownerId, String date);

    @Query("SELECT COUNT(*) FROM checklist_tasks WHERE ownerId = :ownerId AND taskDate = :date AND completed = 1")
    int countCompleted(long ownerId, String date);
}
