package io.geeteshk.sensor.view.ui.table

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.geeteshk.sensor.R
import io.geeteshk.sensor.view.RoundedBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_table.view.*

class TableFragment : RoundedBottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_table, container, false)

        val dataMap = arguments!!.getSerializable("data_map")
        /*val dataMap = mapOf(
                1F to 15F,
                2F to 32.7F,
                3F to 48.9F,
                4F to 50.2F,
                5F to 49.1F,
                6F to 52.3F,
                7F to 52.6F,
                8F to 50F,
                9F to 46.1F,
                10F to 12F,
                11F to 0F,
                12F to 300F,
                13F to 12.5F,
                14F to 1F,
                15F to 87.6F,
                16F to 20F,
                17F to 1F,
                18F to 23.4F,
                19F to 23.7F,
                20F to 24.1F
        )*/
        val viewManager = LinearLayoutManager(activity)
        val viewAdapter = TableAdapter(dataMap as Map<Float, Float>)

        rootView.contentContainer.apply {
            addItemDecoration(DividerItemDecoration(activity, viewManager.orientation))
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        return rootView
    }
}