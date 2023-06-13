package com.aceyan.mytodo.checklist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.aceyan.mytodo.BR
import com.aceyan.mytodo.R
import com.aceyan.mytodo.databinding.LayoutTodoItemBinding
import com.aceyan.mytodo.ui.TodoSetActivity
import com.aceyan.mytodo.utils.EXTRA_TODO_ID
import com.aceyan.mytodo.utils.NO_ID
import com.aceyan.mytodo.view.SmoothCheckBox
import com.ysw.android.extensions.currentTimeMillis
import com.ysw.android.extensions.databinding.AbsBindingRVAdapter
import com.ysw.android.extensions.logE
import com.ysw.android.extensions.mainLaunch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SimpleChecklistAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : AbsBindingRVAdapter<LayoutTodoItemBinding, Todo>() {
    @Deprecated("Use asyncList replaced.", ReplaceWith("asyncList"))
    override val data: MutableList<Todo>
        get() = mutableListOf()

    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    private val asyncList: AsyncListDiffer<Todo> =
        AsyncListDiffer(this, object : DiffUtil.ItemCallback<Todo>() {
            override fun areItemsTheSame(oldItem: Todo, newItem: Todo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Todo, newItem: Todo): Boolean {
                return oldItem == newItem
            }
        })

    private val itemTouchHelper = ItemTouchHelper(ItemTouchCallback())
    private lateinit var recyclerView: RecyclerView

    init {
        setHasStableIds(true)
        lifecycleOwner.lifecycleScope.launch {
            TodoDatabase.queryTodosFlow(context)
                .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.CREATED)
                .onEach {
                    logE { "Db data changed: new=$it" }
                    submitList(it)
                }.launchIn(lifecycleOwner.lifecycleScope)
        }
    }

    override fun bindDefaultData(holder: BindingViewHolder<LayoutTodoItemBinding>, position: Int) {
        val todo = asyncList.currentList[position]
        holder.binding.apply {
            setVariable(getVariableId(), todo)
            timeView.isVisible = todo.hasReminder()

            root.setOnClickListener {
                startTodoSetActivity(holder.itemId)
            }

            checkBox.setChecked(false, todo.done)
            checkBox.animationChangeListener =
                SmoothCheckBox.OnAnimationChangeListener { isChecked, start ->
                    if (!start && isChecked) {
                        mainLaunch {
                            TodoDatabase.deleteTodo(context, todo)
                        }
                    }
                }
        }
    }

    override fun onViewRecycled(holder: BindingViewHolder<LayoutTodoItemBinding>) {
        super.onViewRecycled(holder)
        holder.binding.checkBox.animationChangeListener = null
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        itemTouchHelper.attachToRecyclerView(this.recyclerView)
    }

    override fun getItemCount(): Int {
        return asyncList.currentList.size
    }

    override fun getLayoutRes(): Int {
        return R.layout.layout_todo_item
    }

    override fun getVariableId(): Int {
        return BR.todo
    }

    override fun getItemId(position: Int): Long {
        return asyncList.currentList[position].id
    }

    fun submitList(list: List<Todo>) {
        setAlarmsIfNeed(list)
        asyncList.submitList(list)
    }

    private fun setAlarmsIfNeed(list: List<Todo>) {
        list.filter { it.hasReminder() }.forEach {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + /*(it.reminderTime - currentTimeMillis)*/5000,
                    getAlarmPendingIntent(it)
                )
            }
        }
    }

    private fun getAlarmPendingIntent(todo: Todo) = PendingIntent.getBroadcast(
        context, todo.hashCode(), Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TODO_ID, todo.id)
        },
        PendingIntent.FLAG_IMMUTABLE
    )

    fun startTodoSetActivity(todoId: Long = NO_ID) {
        context.startActivity(
            Intent(context, TodoSetActivity::class.java).apply {
                putExtra(EXTRA_TODO_ID, todoId)
            })
    }

    inner class ItemTouchCallback : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            mainLaunch {
                asyncList.currentList.find { it.id == viewHolder.itemId }?.let {
                    TodoDatabase.deleteTodo(context, it)
                }
            }
        }
    }
}