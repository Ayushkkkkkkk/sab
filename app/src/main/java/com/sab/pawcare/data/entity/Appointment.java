package com.sab.pawcare.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "appointments",
        foreignKeys = @ForeignKey(
                entity = Pet.class,
                parentColumns = "id",
                childColumns = "petId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("petId"), @Index("startsAt")}
)
public class Appointment {
    public static final String TYPE_VET = "Veterinary";
    public static final String TYPE_VACCINATION = "Vaccination";
    public static final String TYPE_GROOMING = "Grooming";
    public static final String TYPE_OTHER = "Other";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long petId;
    public String title;
    public String type;
    public String location;
    public long startsAt;
    public String notes;
    public boolean reminderEnabled;
}
