package com.sab.pawcare.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "pet_photos",
        foreignKeys = @ForeignKey(
                entity = Pet.class,
                parentColumns = "id",
                childColumns = "petId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("petId")}
)
public class PetPhoto {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long petId;
    public String uri;
    public long createdAt;
}
