package com.sab.pawcare.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "checklist_tasks",
        foreignKeys = {
                @ForeignKey(entity = Pet.class, parentColumns = "id", childColumns = "petId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Routine.class, parentColumns = "id", childColumns = "routineId", onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("petId"), @Index("ownerId"), @Index("taskDate"), @Index("routineId")}
)
public class ChecklistTask {
    public static final String SCOPE_DAILY = "DAILY";
    public static final String SCOPE_WEEKLY = "WEEKLY";

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long ownerId;
    public long petId;
    public Long routineId;
    public String title;
    public String category;
    public String notes;
    public String supplies;
    public int scheduledMinutes;
    /** yyyy-MM-dd for daily items; yyyy-'W'ww for weekly bucket */
    public String taskDate;
    public String scope;
    public boolean completed;
    public long completedAt;
    public boolean customTask;
}
