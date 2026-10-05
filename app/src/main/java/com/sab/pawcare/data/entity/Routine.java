package com.sab.pawcare.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "routines",
        foreignKeys = @ForeignKey(
                entity = Pet.class,
                parentColumns = "id",
                childColumns = "petId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("petId")}
)
public class Routine {
    public static final String FREQ_DAILY = "DAILY";
    public static final String FREQ_WEEKLY = "WEEKLY";

    public static final String CAT_FEEDING = "Feeding";
    public static final String CAT_WALKING = "Walking / Exercise";
    public static final String CAT_GROOMING = "Grooming";
    public static final String CAT_MEDICATION = "Medication";
    public static final String CAT_HEALTHCARE = "Healthcare";
    public static final String CAT_CLEANING = "Cleaning";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long petId;
    public String title;
    public String category;
    public String frequency;
    /** Minutes from midnight, e.g. 8:30 -> 510 */
    public int scheduledMinutes;
    /** Bitmask Sun=1, Mon=2, Tue=4, ... Sat=64. Used for weekly. Daily ignores. */
    public int weekdaysMask;
    public String instructions;
    public String supplies;
    public boolean reminderEnabled;
    public long createdAt;
}
