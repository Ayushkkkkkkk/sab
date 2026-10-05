package com.sab.pawcare.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.sab.pawcare.data.entity.Routine;

import java.util.List;

@Dao
public interface RoutineDao {
    @Insert
    long insert(Routine routine);

    @Update
    void update(Routine routine);

    @Delete
    void delete(Routine routine);

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    Routine findById(long id);

    @Query("SELECT * FROM routines WHERE petId = :petId ORDER BY scheduledMinutes, title")
    LiveData<List<Routine>> observeByPet(long petId);

    @Query("SELECT r.* FROM routines r INNER JOIN pets p ON p.id = r.petId WHERE p.ownerId = :ownerId ORDER BY r.scheduledMinutes, r.title")
    LiveData<List<Routine>> observeByOwner(long ownerId);

    @Query("SELECT r.* FROM routines r INNER JOIN pets p ON p.id = r.petId WHERE p.ownerId = :ownerId")
    List<Routine> listByOwner(long ownerId);

    @Query("SELECT * FROM routines WHERE petId = :petId")
    List<Routine> listByPet(long petId);
}
