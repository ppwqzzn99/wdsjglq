package com.mimo.datefaker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mimo.datefaker.R
import com.mimo.datefaker.data.AppInfo

class AppAdapter(
    private val source: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit,
) : RecyclerView.Adapter<AppAdapter.VH>() {

    private var items: List<AppInfo> = source

    fun filter(query: String) {
        val q = query.trim().lowercase()
        items = if (q.isEmpty()) source else source.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.pkg.text = item.packageName
        holder.system.visibility = if (item.isSystem) View.VISIBLE else View.GONE
        try {
            val icon = holder.itemView.context.packageManager.getApplicationIcon(item.packageName)
            holder.icon.setImageDrawable(icon)
        } catch (_: Throwable) {
            holder.icon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.ivIcon)
        val label: TextView = v.findViewById(R.id.tvLabel)
        val pkg: TextView = v.findViewById(R.id.tvPackage)
        val system: TextView = v.findViewById(R.id.tvSystem)
    }
}
