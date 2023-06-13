package com.aceyan.mytodo.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.aceyan.mytodo.checklist.Todo
import com.aceyan.mytodo.checklist.TodoDatabase
import com.aceyan.mytodo.databinding.ActivityTodoSetBinding
import com.aceyan.mytodo.utils.EXTRA_TODO_ID
import com.aceyan.mytodo.utils.NO_ID
import com.aceyan.mytodo.utils.NO_SHOW_EXIT_DIALOG
import com.aceyan.mytodo.utils.TimeUtil
import com.ysw.android.extensions.logE
import com.ysw.android.extensions.mainLaunch
import com.ysw.android.extensions.viewbinding.BaseBindingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Context.getSp() = getSharedPreferences("Checklist_Sp", Context.MODE_PRIVATE)

class TodoSetActivity : BaseBindingActivity<ActivityTodoSetBinding>() {

    private lateinit var target: Todo
    private var reminderTimeStr = StringBuilder()
    private var needUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            val all = TodoDatabase.queryTodos(this@TodoSetActivity)
            val id = intent.getLongExtra(EXTRA_TODO_ID, NO_ID)
            needUpdate = id != NO_ID

            target = if (needUpdate) {
                all.find { it.id == id }!!
            } else {
                Todo()
            }

            mainLaunch {
                with(viewBinding) {
                    if (needUpdate) {
                        contentSet.setText(target.content)
                        timeSet.text = target.getReminderTimeFormat()
                    }

                    timeSet.setOnClickListener {
                        showReminderSetDialog {
                            timeSet.text = it
                        }
                    }

                    toolbar.setNavigationOnClickListener {
                        onBackPressed()
                    }
                }
            }
        }

    }

    private fun setNotShowAnyMoreSp() {
        with(getSp().edit()) {
            putBoolean(NO_SHOW_EXIT_DIALOG, true)
        }
    }

    override fun generateViewBinding(): ActivityTodoSetBinding {
        return ActivityTodoSetBinding.inflate(layoutInflater)
    }

    override fun onBackPressed() {
        if (viewBinding.contentSet.text!!.isEmpty()) {
            if (getSp().getBoolean(NO_SHOW_EXIT_DIALOG, false)) {
                super.onBackPressed()
            } else {
                AlertDialog.Builder(this).apply {
                    setTitle("代办设置")
                    setMessage("当前代办内容为空，退出将不会为您添加到代办列表中")
                    setPositiveButton("退出") { d, w ->
                        d.dismiss()
                        super.onBackPressed()
                    }
                    setNegativeButton("取消") { d, w ->
                        d.dismiss()
                    }
                    setNeutralButton("不在显示") { d, w ->
                        setNotShowAnyMoreSp()
                        d.dismiss()
                        super.onBackPressed()
                    }
                    show()
                }
            }
        } else {
            updateDatabase()
            super.onBackPressed()
        }
    }

    private fun updateTodo() {
        with(viewBinding) {
            target.content = contentSet.text.toString()
            target.reminderTime = TimeUtil.getTimeFromStr(reminderTimeStr.toString())
            logE { "updateTodo: target = $target" }
        }
    }

    private fun updateDatabase() {
        updateTodo()
        lifecycleScope.launch(Dispatchers.IO) {
            if (needUpdate) {
                TodoDatabase.updateTodo(this@TodoSetActivity, target)
            } else {
                TodoDatabase.addTodo(this@TodoSetActivity, target)
            }
        }
    }

    private fun showReminderSetDialog(onComplete: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this@TodoSetActivity,
            { _, year, month, dayOfMonth ->
                reminderTimeStr.append("${year}/${month + 1}/${dayOfMonth} ")
                TimePickerDialog(
                    this@TodoSetActivity,
                    { _, hourOfDay, minute ->
                        reminderTimeStr.append("$hourOfDay:${minute}:00")
                        target.reminderTime = TimeUtil.getTimeFromStr(reminderTimeStr.toString())

                        onComplete(reminderTimeStr.toString())
                    },
                    calendar[Calendar.MINUTE],
                    calendar[Calendar.SECOND],
                    false
                ).show()
            },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        ).show()
    }
}