package net.mullvad.mullvadvpn.ui

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import net.mullvad.mullvadvpn.model.ListItemData
import net.mullvad.mullvadvpn.ui.listitemview.ActionListItemView
import net.mullvad.mullvadvpn.ui.listitemview.ApplicationListItemView
import net.mullvad.mullvadvpn.ui.listitemview.DividerGroupListItemView
import net.mullvad.mullvadvpn.ui.listitemview.ListItemView
import net.mullvad.mullvadvpn.ui.listitemview.PlainListItemView
import net.mullvad.mullvadvpn.ui.listitemview.ProgressListItemView
import net.mullvad.mullvadvpn.ui.listitemview.SearchInputView
import net.mullvad.mullvadvpn.ui.listitemview.SearchListItemView
import net.mullvad.mullvadvpn.ui.listitemview.TwoActionListItemView
import java.util.concurrent.atomic.AtomicLong

class ListItemsAdapter : RecyclerView.Adapter<ListItemsAdapter.ViewHolder>() {

    var listItemListener: ListItemListener? = null

    protected var diffCallback: DiffCallback = DefaultDiffCallback()

    private val listDiffer: AsyncListDiffer<ListItemData> = createDiffer(diffCallback)

    fun setItems(items: List<ListItemData?>) = listDiffer.submitList(items)

    override fun onCreateViewHolder(parent: ViewGroup, @ListItemData.ItemType viewType: Int):
        ListItemsAdapter.ViewHolder {
        return ViewHolder(
            when (viewType) {
                ListItemData.DIVIDER -> DividerGroupListItemView(parent.context)
                ListItemData.PROGRESS -> ProgressListItemView(parent.context)
                ListItemData.PLAIN -> PlainListItemView(parent.context)
                ListItemData.ACTION -> ActionListItemView(parent.context)
                ListItemData.APPLICATION -> ApplicationListItemView(parent.context)
                ListItemData.DOUBLE_ACTION -> TwoActionListItemView(parent.context)
                ListItemData.SEARCH_VIEW -> SearchListItemView(parent.context)
                ListItemData.SEARCH_INPUT_VIEW -> SearchInputView(parent.context)
                else ->
                    throw IllegalArgumentException("View type '$viewType' is not supported")
            }
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.itemView as ListItemView).update(getItem(position))
        holder.itemView.listItemListener = listItemListener
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        (holder.itemView as ListItemView).listItemListener = null
    }

    override fun getItemCount(): Int = listDiffer.currentList.size

    @ListItemData.ItemType
    override fun getItemViewType(position: Int): Int = getItem(position).type

    override fun getItemId(position: Int): Long = getId(getItem(position).identifier)

    private fun getItem(position: Int): ListItemData = listDiffer.currentList[position]

    private fun createDiffer(diffCallback: DiffCallback): AsyncListDiffer<ListItemData> {
        return AsyncListDiffer(getListUpdateCallback(), getConfig(diffCallback))
    }

    private fun getConfig(diffUtil: DiffCallback): AsyncDifferConfig<ListItemData> {
        return AsyncDifferConfig.Builder(diffUtil).build()
    }

    protected fun getListUpdateCallback(): ListUpdateCallback {
        return object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position, count)
                Log.e("TEST", "onInserted: $position $count")
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position, count)
                Log.e("TEST", "onRemoved: $position $count")
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition, toPosition)
                Log.e("TEST", "onMoved: $fromPosition $toPosition")
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                notifyItemRangeChanged(position, count, payload)
                Log.e("TEST", "onChanged: $position $count")
            }
        }
    }

    internal class DefaultDiffCallback : DiffCallback() {
        override fun areItemsTheSame(oldItem: ListItemData, newItem: ListItemData): Boolean {
            if ((oldItem.type == ListItemData.SEARCH_INPUT_VIEW || oldItem.type == ListItemData.SEARCH_VIEW) &&
                (newItem.type == ListItemData.SEARCH_INPUT_VIEW || newItem.type == ListItemData.SEARCH_VIEW) &&
                oldItem.identifier == newItem.identifier
            ) {
                Log.e("TEST", "Items same old: $oldItem")
                Log.e("TEST", "Items same new: $newItem")
            }
            return (oldItem.type == newItem.type && oldItem.identifier == newItem.identifier) ||
                (
                    (oldItem.type == ListItemData.SEARCH_INPUT_VIEW || oldItem.type == ListItemData.SEARCH_VIEW) &&
                        (newItem.type == ListItemData.SEARCH_INPUT_VIEW || newItem.type == ListItemData.SEARCH_VIEW) &&
                        oldItem.identifier == newItem.identifier
                    )
        }

        override fun areContentsTheSame(oldItem: ListItemData, newItem: ListItemData): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: ListItemData, newItem: ListItemData): Any {
            return Any()
        }
    }

    inner class ViewHolder(view: ListItemView) : RecyclerView.ViewHolder(view)

    companion object StableIdProvider {
        private val idCounter = AtomicLong(0)
        private val mapIds = hashMapOf<String, Long>()

        internal fun getId(stringId: String): Long = mapIds.computeIfAbsent(stringId) {
            idCounter.decrementAndGet()
        }
    }
}
typealias DiffCallback = DiffUtil.ItemCallback<ListItemData>
