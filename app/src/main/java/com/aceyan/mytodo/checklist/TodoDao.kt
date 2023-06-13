package com.aceyan.mytodo.checklist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Update
    fun update(todo: Todo)

    @Delete
    fun delete(todo: Todo)

    @Insert
    fun add(todo: Todo)

    @Query("SELECT * FROM todos")
    fun queryAll(): List<Todo>

    @Query("SELECT * FROM todos")
    fun queryAllAsFlow(): Flow<List<Todo>>
}