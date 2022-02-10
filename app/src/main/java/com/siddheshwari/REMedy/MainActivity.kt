package com.siddheshwari.REMedy

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.view.View.OnLongClickListener
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private var mList: RecyclerView? = null
    private var mAdapter: SimpleAdapter? = null
    private var mToolbar: Toolbar? = null
    private var mNoReminderView: TextView? = null
    private var mAddReminderButton: FloatingActionButton? = null
    private var mTempPost = 0
    private val IDmap = LinkedHashMap<Int, Int>()
    private var rb: ReminderDatabase? = null
    private val mMultiSelector: MultiSelector = MultiSelector()
    private var mAlarmReceiver: AlarmReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize reminder database
        rb = ReminderDatabase(applicationContext)

        // Initialize views
        mToolbar = findViewById<View>(R.id.toolbar) as Toolbar
        mAddReminderButton = findViewById<View>(R.id.add_reminder) as FloatingActionButton
        mList = findViewById<View>(R.id.reminder_list) as RecyclerView
        mNoReminderView = findViewById<View>(R.id.no_reminder_text) as TextView

        // To check is there are saved reminders
        // If there are no reminders display a message asking the user to create reminders
        val mTest: List<Reminder> = rb.getAllReminders()
        if (mTest.isEmpty()) {
            mNoReminderView!!.visibility = View.VISIBLE
        }

        // Create recycler view
        mList!!.layoutManager = layoutManager
        registerForContextMenu(mList)
        mAdapter = SimpleAdapter()
        mAdapter!!.itemCount = defaultItemCount
        mList!!.adapter = mAdapter

        // Setup toolbar
        setSupportActionBar(mToolbar)
        mToolbar.setTitle(R.string.app_name)

        // On clicking the floating action button
        mAddReminderButton!!.setOnClickListener { v ->
            val intent = Intent(v.context, ReminderAddActivity::class.java)
            startActivity(intent)
        }

        // Initialize alarm
        mAlarmReceiver = AlarmReceiver()
    }

    // Create context menu for long press actions
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        menuInflater.inflate(R.menu.menu_add_reminder, menu)
    }

    // Multi select items in recycler view
    private val mDeleteMode: android.support.v7.view.ActionMode.Callback =
        object : ModalMultiSelectorCallback(mMultiSelector) {
            fun onCreateActionMode(
                actionMode: android.support.v7.view.ActionMode?,
                menu: Menu?
            ): Boolean {
                menuInflater.inflate(R.menu.menu_add_reminder, menu)
                return true
            }

            fun onActionItemClicked(
                actionMode: android.support.v7.view.ActionMode,
                menuItem: MenuItem
            ): Boolean {
                when (menuItem.itemId) {
                    R.id.discard_reminder -> {
                        // Close the context menu
                        actionMode.finish()

                        // Get the reminder id associated with the recycler view item
                        var i = IDmap.size
                        while (i >= 0) {
                            if (mMultiSelector.isSelected(i, 0)) {
                                val id = IDmap[i]!!

                                // Get reminder from reminder database using id
                                val temp: Reminder = rb.getReminder(id)
                                // Delete reminder
                                rb.deleteReminder(temp)
                                // Remove reminder from recycler view
                                mAdapter!!.removeItemSelected(i)
                                // Delete reminder alarm
                                mAlarmReceiver.cancelAlarm(applicationContext, id)
                            }
                            i--
                        }

                        // Clear selected items in recycler view
                        mMultiSelector.clearSelections()
                        // Recreate the recycler items
                        // This is done to remap the item and reminder ids
                        mAdapter!!.onDeleteItem(defaultItemCount)

                        // Display toast to confirm delete
                        Toast.makeText(
                            applicationContext,
                            "Deleted",
                            Toast.LENGTH_SHORT
                        ).show()

                        // To check is there are saved reminders
                        // If there are no reminders display a message asking the user to create reminders
                        val mTest: List<Reminder> = rb.getAllReminders()
                        if (mTest.isEmpty()) {
                            mNoReminderView!!.visibility = View.VISIBLE
                        } else {
                            mNoReminderView!!.visibility = View.GONE
                        }
                        return true
                    }
                    R.id.save_reminder -> {
                        // Close the context menu
                        actionMode.finish()
                        // Clear selected items in recycler view
                        mMultiSelector.clearSelections()
                        return true
                    }
                    else -> {}
                }
                return false
            }
        }

    // On clicking a reminder item
    private fun selectReminder(mClickID: Int) {
        val mStringClickID = Integer.toString(mClickID)

        // Create intent to edit the reminder
        // Put reminder id as extra
        val i = Intent(this, ReminderEditActivity::class.java)
        i.putExtra(ReminderEditActivity.EXTRA_REMINDER_ID, mStringClickID)
        startActivityForResult(i, 1)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mAdapter!!.itemCount = defaultItemCount
    }

    // Recreate recycler view
    // This is done so that newly created reminders are displayed
    public override fun onResume() {
        super.onResume()

        // To check is there are saved reminders
        // If there are no reminders display a message asking the user to create reminders
        val mTest: List<Reminder> = rb.getAllReminders()
        if (mTest.isEmpty()) {
            mNoReminderView!!.visibility = View.VISIBLE
        } else {
            mNoReminderView!!.visibility = View.GONE
        }
        mAdapter!!.itemCount = defaultItemCount
    }

    // Layout manager for recycler view
    protected val layoutManager: RecyclerView.LayoutManager
        protected get() = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

    protected val defaultItemCount: Int
        protected get() = 100

    // Create menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Setup menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_licenses -> {
                val intent = Intent(this, LicencesActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Adapter class for recycler view
    inner class SimpleAdapter : RecyclerView.Adapter<SimpleAdapter.VerticalItemHolder>() {
        private val mItems: ArrayList<ReminderItem>
        fun setItemCount(count: Int) {
            mItems.clear()
            mItems.addAll(generateData(count))
            notifyDataSetChanged()
        }

        fun onDeleteItem(count: Int) {
            mItems.clear()
            mItems.addAll(generateData(count))
        }

        fun removeItemSelected(selected: Int) {
            if (mItems.isEmpty()) return
            mItems.removeAt(selected)
            notifyItemRemoved(selected)
        }

        // View holder for recycler view items
        override fun onCreateViewHolder(container: ViewGroup, viewType: Int): VerticalItemHolder {
            val inflater = LayoutInflater.from(container.context)
            val root: View = inflater.inflate(R.layout.recycle_items, container, false)
            return VerticalItemHolder(root, this)
        }

        override fun onBindViewHolder(itemHolder: VerticalItemHolder, position: Int) {
            val item = mItems[position]
            itemHolder.setReminderTitle(item.mTitle)
            itemHolder.setReminderDateTime(item.mDateTime)
            itemHolder.setReminderRepeatInfo(item.mRepeat, item.mRepeatNo, item.mRepeatType)
            itemHolder.setActiveImage(item.mActive)
        }

        override fun getItemCount(): Int {
            return mItems.size
        }

        // Class for recycler view items
        inner class ReminderItem(
            var mTitle: String,
            var mDateTime: String,
            var mRepeat: String,
            var mRepeatNo: String,
            var mRepeatType: String,
            var mActive: String
        )

        // Class to compare date and time so that items are sorted in ascending order
        inner class DateTimeComparator : Comparator<Any?> {
            var f: DateFormat = SimpleDateFormat("dd/mm/yyyy hh:mm")
            override fun compare(a: Any, b: Any): Int {
                val o1: String = (a as DateTimeSorter).getDateTime()
                val o2: String = (b as DateTimeSorter).getDateTime()
                return try {
                    f.parse(o1).compareTo(f.parse(o2))
                } catch (e: ParseException) {
                    throw IllegalArgumentException(e)
                }
            }
        }

        // UI and data class for recycler view items
        inner class VerticalItemHolder(itemView: View, adapter: SimpleAdapter) :
            SwappingHolder(itemView, mMultiSelector), View.OnClickListener,
            OnLongClickListener {
            private val mTitleText: TextView
            private val mDateAndTimeText: TextView
            private val mRepeatInfoText: TextView
            private val mActiveImage: ImageView
            private val mThumbnailImage: ImageView
            private val mColorGenerator: ColorGenerator = ColorGenerator.DEFAULT
            private var mDrawableBuilder: TextDrawable? = null
            private val mAdapter: SimpleAdapter

            // On clicking a reminder item
            override fun onClick(v: View) {
                if (!mMultiSelector.tapSelection(this)) {
                    mTempPost = mList!!.getChildAdapterPosition(v)
                    val mReminderClickID = IDmap[mTempPost]!!
                    selectReminder(mReminderClickID)
                } else if (mMultiSelector.getSelectedPositions().isEmpty()) {
                    mAdapter.itemCount = defaultItemCount
                }
            }

            // On long press enter action mode with context menu
            override fun onLongClick(v: View): Boolean {
                val activity: AppCompatActivity = this@MainActivity
                activity.startSupportActionMode(mDeleteMode)
                mMultiSelector.setSelected(this, true)
                return true
            }

            // Set reminder title view
            fun setReminderTitle(title: String?) {
                mTitleText.text = title
                var letter = "A"
                if (title != null && !title.isEmpty()) {
                    letter = title.substring(0, 1)
                }
                val color: Int = mColorGenerator.getRandomColor()

                // Create a circular icon consisting of  a random background colour and first letter of title
                mDrawableBuilder = TextDrawable.builder()
                    .buildRound(letter, color)
                mThumbnailImage.setImageDrawable(mDrawableBuilder)
            }

            // Set date and time views
            fun setReminderDateTime(datetime: String?) {
                mDateAndTimeText.text = datetime
            }

            // Set repeat views
            fun setReminderRepeatInfo(repeat: String, repeatNo: String, repeatType: String) {
                if (repeat == "true") {
                    mRepeatInfoText.text = "Every $repeatNo $repeatType(s)"
                } else if (repeat == "false") {
                    mRepeatInfoText.text = "Repeat Off"
                }
            }

            // Set active image as on or off
            fun setActiveImage(active: String) {
                if (active == "true") {
                    mActiveImage.setImageResource(R.drawable.ic_notifications_on_white_24dp)
                } else if (active == "false") {
                    mActiveImage.setImageResource(R.drawable.ic_notifications_off_grey600_24dp)
                }
            }

            init {
                itemView.setOnClickListener(this)
                itemView.setOnLongClickListener(this)
                itemView.isLongClickable = true

                // Initialize adapter for the items
                mAdapter = adapter

                // Initialize views
                mTitleText = itemView.findViewById<View>(R.id.recycle_title) as TextView
                mDateAndTimeText = itemView.findViewById<View>(R.id.recycle_date_time) as TextView
                mRepeatInfoText = itemView.findViewById<View>(R.id.recycle_repeat_info) as TextView
                mActiveImage = itemView.findViewById<View>(R.id.active_image) as ImageView
                mThumbnailImage = itemView.findViewById<View>(R.id.thumbnail_image) as ImageView
            }
        }

        // Generate random test data
        fun generateDummyData(): ReminderItem {
            return ReminderItem("1", "2", "3", "4", "5", "6")
        }

        // Generate real data for each item
        fun generateData(count: Int): List<ReminderItem> {
            val items = ArrayList<ReminderItem>()

            // Get all reminders from the database
            val reminders: List<Reminder> = rb.getAllReminders()

            // Initialize lists
            val Titles: MutableList<String> = ArrayList()
            val Repeats: MutableList<String> = ArrayList()
            val RepeatNos: MutableList<String> = ArrayList()
            val RepeatTypes: MutableList<String> = ArrayList()
            val Actives: MutableList<String> = ArrayList()
            val DateAndTime: MutableList<String> = ArrayList()
            val IDList: MutableList<Int> = ArrayList()
            val DateTimeSortList: MutableList<DateTimeSorter> = ArrayList<DateTimeSorter>()

            // Add details of all reminders in their respective lists
            for (r in reminders) {
                Titles.add(r.getTitle())
                DateAndTime.add(r.getDate().toString() + " " + r.getTime())
                Repeats.add(r.getRepeat())
                RepeatNos.add(r.getRepeatNo())
                RepeatTypes.add(r.getRepeatType())
                Actives.add(r.getActive())
                IDList.add(r.getID())
            }
            var key = 0

            // Add date and time as DateTimeSorter objects
            for (k in Titles.indices) {
                DateTimeSortList.add(DateTimeSorter(key, DateAndTime[k]))
                key++
            }

            // Sort items according to date and time in ascending order
            Collections.sort(DateTimeSortList, DateTimeComparator())
            var k = 0

            // Add data to each recycler view item
            for (item in DateTimeSortList) {
                val i: Int = item.getIndex()
                items.add(
                    ReminderItem(
                        Titles[i], DateAndTime[i], Repeats[i],
                        RepeatNos[i], RepeatTypes[i], Actives[i]
                    )
                )
                IDmap[k] = IDList[i]
                k++
            }
            return items
        }

        init {
            mItems = ArrayList()
        }
    }
}