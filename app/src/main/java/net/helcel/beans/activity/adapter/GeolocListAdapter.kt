package net.helcel.beans.activity.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import net.helcel.beans.activity.fragment.EditPlaceColorFragment
import net.helcel.beans.activity.fragment.EditPlaceFragment
import net.helcel.beans.countries.GeoLoc
import net.helcel.beans.databinding.ItemListGeolocBinding
import net.helcel.beans.helper.*
import net.helcel.beans.helper.Theme.colorWrapper

class GeolocListAdapter(
    private val ctx: EditPlaceFragment, private val l: GeoLoc, private val pager: ViewPagerAdapter,
    private val parentHolder: FoldingListViewHolder?
) : RecyclerView.Adapter<GeolocListAdapter.FoldingListViewHolder>() {

    private val sortedList = l.children.toList().sortedBy { it.fullName }
    private val holders: MutableSet<FoldingListViewHolder> = mutableSetOf()

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): FoldingListViewHolder {
        val binding = ItemListGeolocBinding.inflate(
            LayoutInflater.from(viewGroup.context),
            viewGroup,
            false
        )
        val holder = FoldingListViewHolder(ctx.requireActivity(), binding, parentHolder, l)
        holders.add(holder)
        return holder
    }

    override fun onBindViewHolder(holder: FoldingListViewHolder, position: Int) {
        val el = sortedList[position]
        holder.bind(el)
        holder.addListeners(el) {
            if (el.children.isNotEmpty())
                pager.addFragment(ctx, EditPlaceFragment(el, pager, holder))
            true
        }
    }

    override fun getItemCount(): Int {
        return l.children.size
    }

    fun refreshColors(colorDrawable: ColorDrawable) {
        holders.forEach { it.refreshColor(colorDrawable) }
    }

    class FoldingListViewHolder(
        private val ctx: FragmentActivity,
        private val _binding: ItemListGeolocBinding,
        private val _parentHolder: FoldingListViewHolder? = null,
        private val _parentGeoLoc: GeoLoc,
    ) : RecyclerView.ViewHolder(_binding.root), DialogCloser {
        private lateinit var el: GeoLoc

        private fun bindGroup(el: GeoLoc) {
            refreshCount(el)
            _binding.textView.setTypeface(null, Typeface.BOLD)
            _binding.textView.backgroundTintList = ColorStateList.valueOf(
                colorWrapper(
                    ctx,
                    android.R.attr.panelColorBackground
                ).color
            ).withAlpha(64)
        }

        fun bind(el: GeoLoc) {
            this.el = el
            _binding.textView.text = el.fullName
            _binding.textView.backgroundTintList =
                ColorStateList.valueOf(colorWrapper(ctx, android.R.attr.colorBackground).color)

            if (el.children.isNotEmpty())
                bindGroup(el)

            refreshCheck(el)
        }

        fun refreshColor(colorDrawable: ColorDrawable) {
            if (Data.visits.getVisited(el) !in listOf(NO_GROUP, AUTO_GROUP)) {
                _binding.checkBox.buttonTintList =
                    ColorStateList.valueOf(colorDrawable.color)
                refreshCheck(el)
            }
        }

        fun addListeners(el: GeoLoc, expandLambda: () -> Boolean) {
            if (el.children.isNotEmpty()) {
                _binding.textView.setOnClickListener { expandLambda() }
            }
            _binding.checkBox.setOnClickListener {
                Data.selected_geoloc = el
                if (Data.groups.size() == 1 && Settings.isSingleGroup(ctx)) {
                    if (_binding.checkBox.isChecked) {
                        // If one has just checked the box (assign unique group)
                        Data.selected_group = Data.groups.getUniqueEntry()
                        onDialogDismiss(false)
                    } else {
                        // If one has just unchecked the box (unassign unique group)
                        Data.selected_group = null
                        onDialogDismiss(true)
                    }
                } else {
                    Data.selected_group = null
                    EditPlaceColorFragment(this).show(
                        ctx.supportFragmentManager,
                        "AddColorDialogFragment"
                    )
                }
                _parentHolder?.refresh(_parentGeoLoc)
            }
        }

        override fun onDialogDismiss(clear: Boolean) {
            if (clear) {
                Data.visits.setVisited(Data.selected_geoloc, NO_GROUP)
                Data.saveData()

                if (_parentGeoLoc.children.all { Data.visits.getVisited(it) == NO_GROUP }) {
                    Data.clearing_geoloc = _parentGeoLoc
                }
            }
            if (Data.selected_group != null && Data.selected_geoloc != null) {
                Data.visits.setVisited(Data.selected_geoloc, Data.selected_group?.key ?: NO_GROUP)
                Data.saveData()
            }
            Data.selected_geoloc?.let { refreshCheck(it) }
            Data.selected_geoloc = null
            Data.selected_group = null
            _parentHolder?.refresh(_parentGeoLoc)
        }

        private fun refreshCheck(geoLoc: GeoLoc) {
            _binding.checkBox.checkedState =
                if (Data.visits.getVisited(geoLoc) !in listOf(NO_GROUP, AUTO_GROUP)) {
                    MaterialCheckBox.STATE_CHECKED
                } else if (geoLoc.children.isNotEmpty() &&
                    geoLoc.children.all {
                        Data.visits.getVisited(it) !in listOf(NO_GROUP, AUTO_GROUP)
                    }
                ) {
                    Data.visits.setVisited(geoLoc, AUTO_GROUP)
                    MaterialCheckBox.STATE_CHECKED
                } else if (geoLoc.children.isEmpty() && Data.visits.getVisited(geoLoc) == AUTO_GROUP) {
                    MaterialCheckBox.STATE_CHECKED
                } else if (geoLoc.children.any { Data.visits.getVisited(it) != NO_GROUP }) {
                    Data.visits.setVisited(geoLoc, AUTO_GROUP)
                    MaterialCheckBox.STATE_INDETERMINATE
                } else {
                    Data.visits.setVisited(geoLoc, NO_GROUP)
                    if (Data.clearing_geoloc == geoLoc) {
                        Data.clearing_geoloc = null
                    }
                    MaterialCheckBox.STATE_UNCHECKED
                }
            Data.saveData()

            var col = Data.groups.getGroupFromKey(Data.visits.getVisited(geoLoc)).color
            if (Data.visits.getVisited(geoLoc) == AUTO_GROUP) {
                col = colorWrapper(ctx, android.R.attr.colorPrimary)
            } else if (col.color == Color.TRANSPARENT) {
                col = colorWrapper(ctx, android.R.attr.panelColorBackground)
                col.alpha = 64
            }
            _binding.checkBox.buttonTintList = ColorStateList.valueOf(col.color)
        }

        private fun refreshCount(geoLoc: GeoLoc) {
            val numerator =
                geoLoc.children.map { Data.visits.getVisited(it) != NO_GROUP }.count { it }
            val denominator = geoLoc.children.size
            _binding.count.text = Settings.getStats(ctx, numerator, denominator)
        }

        private fun refresh(geoLoc: GeoLoc) {
            // Refresh
            refreshCheck(geoLoc)
            refreshCount(geoLoc)

            // Recursively refresh parent
            _parentHolder?.refresh(_parentGeoLoc)
        }

    }
}
