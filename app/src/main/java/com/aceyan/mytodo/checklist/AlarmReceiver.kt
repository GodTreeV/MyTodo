package com.aceyan.mytodo.checklist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aceyan.mytodo.utils.EXTRA_TODO_ID
import com.aceyan.mytodo.utils.NO_ID
import com.ysw.android.extensions.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, NO_ID)
        CoroutineScope(Dispatchers.IO).launch {
            val all = TodoDatabase.queryTodos(context)
            all.find { it.id == todoId }?.let {
                if (!it.done) {
                    TodoDatabase.deleteTodo(context, it)
                }
            }
        }
    }
}