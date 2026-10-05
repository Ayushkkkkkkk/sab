package com.sab.pawcare.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.sab.pawcare.data.entity.Pet;

import java.util.List;

@Dao
public interface PetDao {
    @Insert
    long insert(Pet pet);

    @Update
    void update(Pet pet);

    @Delete
    void delete(Pet pet);

    @Query("SELECT * FROM pets WHERE ownerId = :ownerId ORDER BY name COLLATE NOCASE")
    LiveData<List<Pet>> observeByOwner(long ownerId);

    @Query("SELECT * FROM pets WHERE ownerId = :ownerId ORDER BY name COLLATE NOCASE")
    List<Pet> listByOwner(long ownerId);

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    Pet findById(long id);

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    LiveData<Pet> observeById(long id);
}
