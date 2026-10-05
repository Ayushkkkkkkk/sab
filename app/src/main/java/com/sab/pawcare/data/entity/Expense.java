package com.sab.pawcare.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "expenses",
        foreignKeys = @ForeignKey(
                entity = Pet.class,
                parentColumns = "id",
                childColumns = "petId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("petId"), @Index("spentAt")}
)
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long petId;
    public String title;
    public String category;
    public double amount;
    public long spentAt;
    public String notes;
}
