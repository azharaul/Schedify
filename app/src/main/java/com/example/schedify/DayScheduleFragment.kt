package com.example.schedify

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DayScheduleFragment : Fragment() {

    private val viewModel: ScheduleViewModel by activityViewModels()
    private lateinit var dayName: String
    private lateinit var rvDaySchedules: RecyclerView
    private lateinit var adapter: ScheduleListAdapter

    companion object {
        private const val ARG_DAY = "arg_day"

        fun newInstance(day: String): DayScheduleFragment {
            val f = DayScheduleFragment()
            val args = Bundle()
            args.putString(ARG_DAY, day)
            f.arguments = args
            return f
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        arguments?.let {
            dayName = it.getString(ARG_DAY) ?: ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_day_schedule, container, false)
        rvDaySchedules = v.findViewById(R.id.rvDaySchedules)
        rvDaySchedules.layoutManager = LinearLayoutManager(requireContext())
        adapter = ScheduleListAdapter(
            onEditClick = { schedule ->
                (activity as? MainActivity)?.let { act ->
                    AddScheduleDialog(act,
                        { updated -> act.viewModel.updateSchedule(updated); act.showSnackbar(act.getString(R.string.msg_updated)) },
                        { toDelete -> act.deleteWithUndo(toDelete) }
                    ).show(schedule)
                }
            },
            onDeleteClick = { schedule ->
                (activity as? MainActivity)?.deleteWithUndo(schedule)
            }
        )
        rvDaySchedules.adapter = adapter
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe schedules and filter by day
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allSchedules.collect { schedules ->
                val filtered = schedules.filter { it.day == dayName }
                adapter.submitList(filtered)
            }
        }
    }
}
