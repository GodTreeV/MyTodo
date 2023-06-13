package com.aceyan.mytodo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aceyan.mytodo.checklist.SimpleChecklistAdapter
import com.aceyan.mytodo.checklist.Todo
import com.aceyan.mytodo.checklist.TodoDatabase
import com.aceyan.mytodo.databinding.ActivityMainBinding
import com.google.android.material.search.SearchView
import com.ysw.android.extensions.currentTimeMillis
import com.ysw.android.extensions.mainLaunch
import com.ysw.android.extensions.viewbinding.BaseBindingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainActivity : BaseBindingActivity<ActivityMainBinding>() {

    private lateinit var checklistAdapter: SimpleChecklistAdapter

    override fun generateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        checklistAdapter = SimpleChecklistAdapter(this, this)

        with(viewBinding) {
            lifecycleScope.launch(Dispatchers.IO) {
                TodoDatabase.getInstance(this@MainActivity).todoDao().queryAllAsFlow()
                    .flowWithLifecycle(
                        lifecycle,
                        Lifecycle.State.CREATED
                    ).onEach {
                        noChecklist.isVisible = it.isEmpty()
                        ViewCompat.setNestedScrollingEnabled(checklistRv, it.isNotEmpty())
                    }.launchIn(lifecycleScope)
            }

            checklistRv.apply {
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = checklistAdapter
            }

            fab.setOnClickListener {
                checklistAdapter.startTodoSetActivity()
            }

            searchView.addTransitionListener { _, _, newState ->
                if (newState == SearchView.TransitionState.SHOWN) {
                    fab.hide()
                }
                if (newState == SearchView.TransitionState.HIDDEN) {
                    fab.show()
                }
            }
        }
    }

    private fun testAddTodo() {
        for (index in 0..20) {
            mainLaunch {
                TodoDatabase.addTodo(
                    this@MainActivity, Todo(
                        id = index.toLong(),
                        content = "Test $index",
                        reminderTime = currentTimeMillis
                    )
                )
            }
        }
    }
}