package com.sab.pawcare.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.sab.pawcare.data.entity.Appointment;

import java.util.List;

@Dao
public interface AppointmentDao {
    @Insert
    long insert(Appointment appointment);

    @Update
    void update(Appointment appointment);

    @Delete
    void delete(Appointment appointment);

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    Appointment findById(long id);

    @Query("SELECT a.* FROM appointments a INNER JOIN pets p ON p.id = a.petId WHERE p.ownerId = :ownerId ORDER BY a.startsAt ASC")
    LiveData<List<Appointment>> observeByOwner(long ownerId);

    @Query("SELECT * FROM appointments WHERE petId = :petId ORDER BY startsAt ASC")
    LiveData<List<Appointment>> observeByPet(long petId);

    @Query("SELECT a.* FROM appointments a INNER JOIN pets p ON p.id = a.petId WHERE p.ownerId = :ownerId AND a.startsAt >= :from ORDER BY a.startsAt ASC")
    List<Appointment> upcoming(long ownerId, long from);
}
