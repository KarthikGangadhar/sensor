package io.geeteshk.sensor.view

import android.app.Dialog
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import io.geeteshk.sensor.R

open class RoundedBottomSheetDialogFragment : BottomSheetDialogFragment() {

    override fun getTheme() = R.style.BottomSheet_DialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            BottomSheetDialog(requireContext(), theme)
}