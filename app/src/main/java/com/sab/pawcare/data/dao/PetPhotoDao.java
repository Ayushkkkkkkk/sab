package com.sab.pawcare.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.sab.pawcare.data.entity.PetPhoto;

import java.util.List;

@Dao
public interface PetPhotoDao {
    @Insert
    long insert(PetPhoto photo);

    @Delete
    void delete(PetPhoto photo);

    @Query("SELECT * FROM pet_photos WHERE petId = :petId ORDER BY createdAt DESC")
    LiveData<List<PetPhoto>> observeByPet(long petId);

    @Query("SELECT * FROM pet_photos WHERE petId = :petId ORDER BY createdAt DESC")
    List<PetPhoto> listByPet(long petId);
}
