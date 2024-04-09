package net.helcel.beans.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import net.helcel.beans.R
import net.helcel.beans.activity.adapter.StatsListAdapter
import net.helcel.beans.databinding.ActivityStatBinding
import net.helcel.beans.helper.Theme.createActionBar

private val MODE_LIST = listOf("World", "Country", "Region")

class StatActivity : AppCompatActivity() {

    private lateinit var _binding: ActivityStatBinding
    private var activeMode: String = "World"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityStatBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        createActionBar(this, getString(R.string.action_stat))
        
        _binding.stats.layoutManager =
            LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val adapter = StatsListAdapter(_binding.stats)
        _binding.stats.adapter = adapter

        _binding.pager.adapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int = 3
            override fun createFragment(position: Int): Fragment = Fragment()
        }
        TabLayoutMediator(_binding.tab, _binding.pager) { tab, position ->
            tab.text = MODE_LIST[position]
        }.attach()

        _binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activeMode = MODE_LIST[position]
                adapter.refreshMode(activeMode)
            }
        })
        adapter.refreshMode(activeMode)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

}