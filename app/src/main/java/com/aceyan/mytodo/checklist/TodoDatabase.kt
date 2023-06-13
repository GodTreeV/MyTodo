package com.aceyan.mytodo.checklist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ysw.android.extensions.withIoContext
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        Todo::class
    ],
    version = 1
)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): TodoDatabase {
            return synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        TodoDatabase::class.java,
                        "Checklist.db"
                    ).build()
                }
                INSTANCE!!
            }
        }

        @JvmStatic
        suspend fun deleteTodo(context: Context, todo: Todo) {
            withIoContext {
                getInstance(context).todoDao().delete(todo)
            }
        }

        @JvmStatic
        suspend fun addTodo(context: Context, todo: Todo) {
            withIoContext {
                getInstance(context).todoDao().add(todo)
            }
        }

        @JvmStatic
        suspend fun updateTodo(context: Context, todo: Todo) {
            withIoContext {
                getInstance(context).todoDao().update(todo)
            }
        }

        @JvmStatic
        suspend fun queryTodos(context: Context): List<Todo> {
            return withIoContext {
                getInstance(context).todoDao().queryAll()
            }
        }

        @JvmStatic
        suspend fun queryTodosFlow(context: Context): Flow<List<Todo>> {
            return withIoContext {
                getInstance(context).todoDao().queryAllAsFlow()
            }
        }
    }
}