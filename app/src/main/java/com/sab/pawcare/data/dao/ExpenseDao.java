package com.sab.pawcare.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.sab.pawcare.data.entity.Expense;

import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert
    long insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("SELECT * FROM expenses WHERE petId = :petId ORDER BY spentAt DESC")
    LiveData<List<Expense>> observeByPet(long petId);

    @Query("SELECT e.* FROM expenses e INNER JOIN pets p ON p.id = e.petId WHERE p.ownerId = :ownerId ORDER BY e.spentAt DESC")
    LiveData<List<Expense>> observeByOwner(long ownerId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM expenses e INNER JOIN pets p ON p.id = e.petId WHERE p.ownerId = :ownerId AND e.spentAt BETWEEN :from AND :to")
    double sumBetween(long ownerId, long from, long to);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE petId = :petId AND spentAt BETWEEN :from AND :to")
    double sumForPetBetween(long petId, long from, long to);

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    Expense findById(long id);
}
