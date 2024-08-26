package com.imthedriver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.imthedriver.R

class TipsAdapter(private val tips: List<Tip>) : RecyclerView.Adapter<TipsAdapter.TipsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tip, parent, false)
        return TipsViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipsViewHolder, position: Int) {
        holder.bind(tips[position])
    }

    override fun getItemCount(): Int = tips.size

    class TipsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tipTextView: TextView = itemView.findViewById(R.id.tipTextView)
        private val tipImageView: ImageView = itemView.findViewById(R.id.tipImageView)

        fun bind(tip: Tip) {
            tipTextView.text = tip.text
            tipImageView.setImageResource(tip.imageResId)
        }
    }
}

data class Tip(val text: String, val imageResId: Int)
