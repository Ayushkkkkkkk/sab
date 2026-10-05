package com.sab.pawcare.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
public class User {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String fullName;
    public String email;
    public String passwordHash;
    public long createdAt;
}
