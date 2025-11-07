package com.Plant_application.ui.calendar

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.Plant_application.R
import com.Plant_application.data.database.CalendarTask
import com.Plant_application.databinding.ItemTodoBinding

class TodoAdapter(
    private val onTaskClick: (CalendarTask) -> Unit,
    private val onTaskLongClick: (CalendarTask) -> Unit,
    private val isDeleteMode: () -> Boolean,
    private val isSelected: (Long) -> Boolean,
    private val isEditable: (Long) -> Boolean
) : ListAdapter<CalendarTask, TodoAdapter.TodoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position), onTaskClick, onTaskLongClick, isDeleteMode(), isSelected(getItem(position).id), isEditable(getItem(position).dueDate))
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val item = getItem(position)
            for (payload in payloads) {
                if (payload == "DELETE_MODE_CHANGED") holder.updateDeleteModeUI(isDeleteMode(), isSelected(item.id))
                if (payload == "SELECTION_CHANGED") holder.updateDeleteModeUI(isDeleteMode(), isSelected(item.id))
            }
        }
    }

    class TodoViewHolder(private val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            task: CalendarTask,
            onClick: (CalendarTask) -> Unit,
            onLongClick: (CalendarTask) -> Unit,
            isDelete: Boolean,
            isSelected: Boolean,
            isEditable: Boolean
        ) {
            binding.tvTodoTitle.text = task.title

            updateCheckedState(task.isCompleted)
            binding.checkboxTodo.isEnabled = isEditable

            binding.checkboxTodo.setOnCheckedChangeListener(null)
            binding.checkboxTodo.isChecked = task.isCompleted

            binding.checkboxTodo.setOnCheckedChangeListener { _, _ ->
                if (!isDelete) {
                    onClick(task)
                }
            }

            itemView.setOnClickListener {
                if (isDelete) {
                    onLongClick(task)
                } else if (isEditable) {
                    onClick(task)
                }
            }

            itemView.setOnLongClickListener {
                if (!isDelete) {
                    onLongClick(task)
                }
                true
            }

            updateDeleteModeUI(isDelete, isSelected)
        }

        private fun updateCheckedState(isCompleted: Boolean) {
            if (isCompleted) {
                binding.tvTodoTitle.paintFlags = binding.tvTodoTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTodoTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            } else {
                binding.tvTodoTitle.paintFlags = binding.tvTodoTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTodoTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            }
        }

        fun updateDeleteModeUI(isDelete: Boolean, isSelected: Boolean) {
            binding.checkboxTodo.isVisible = !isDelete
            binding.ivDeleteCheckbox.isVisible = isDelete

            if (isDelete) {
                binding.ivDeleteCheckbox.setImageResource(
                    if (isSelected) R.drawable.ic_checkbox_checked_custom else R.drawable.ic_checkbox_unchecked_custom
                )
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<CalendarTask>() {
            override fun areItemsTheSame(oldItem: CalendarTask, newItem: CalendarTask): Boolean {
                return oldItem.id == newItem.id
            }
            override fun areContentsTheSame(oldItem: CalendarTask, newItem: CalendarTask): Boolean {
                return oldItem == newItem
            }
        }
    }
}