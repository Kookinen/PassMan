package com.example.passman

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

// Adapter class for the textView so that the elements (passwords) can be chosen from the list.
// This class is built with some help from the internet and AI.
class Adapter(
    private val context: Context,
    private val items: ArrayList<Item>
)  : BaseAdapter(){
    private val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return items.size
    }
    override fun getItem(position: Int): Item? {
        return if (position in 0 until items.size) items[position] else null
    }
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_layout, parent, false)
        val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
        val item = getItem(position)
        if(item != null){
            nameTextView.text = item.name
        }

        return view
    }
}
