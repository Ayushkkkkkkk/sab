package com.sab.pawcare.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "pets",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "ownerId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("ownerId")}
)
public class Pet {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long ownerId;
    public String name;
    public String animalType;
    public String breed;
    public int ageYears;
    public double weightKg;
    public String allergies;
    public String dietaryPreferences;
    public String favouriteToys;
    public String medicalNotes;
    public String vaccinationHistory;
    public String photoUri;
    public long createdAt;
}
